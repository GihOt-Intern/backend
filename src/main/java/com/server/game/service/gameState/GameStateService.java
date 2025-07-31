package com.server.game.service.gameState;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.component.attackComponent.SkillReceivable;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.map.shape.Shape;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.respawn.ChampionDeathSend;
import com.server.game.netty.sendObject.respawn.ChampionRespawnSend;
import com.server.game.netty.sendObject.respawn.ChampionRespawnTimeSend;
import com.server.game.service.attack.AttackTargetingService;
import com.server.game.util.Util;
import com.server.game.model.game.Entity;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GameStateService {
    
    // gameId -> GameState
    private final Map<String, GameState> gameStates = new ConcurrentHashMap<>();
    
    // Track active respawn schedulers to prevent duplicates: gameId:slot -> scheduler
    private final Map<String, ScheduledExecutorService> activeRespawnSchedulers = new ConcurrentHashMap<>();

    private GameCoordinator gameCoordinator;
    private AttackTargetingService attackTargetingService;

    public GameStateService(
        @Lazy GameCoordinator gameCoordinator, 
        @Lazy AttackTargetingService attackTargetingService) {
        this.gameCoordinator = gameCoordinator;
        this.attackTargetingService = attackTargetingService;
    }

    /**
     * Initialize game state for a specific game
     */
    public void register(GameState gameState) {
        gameStates.put(gameState.getGameId(), gameState);
        log.info("Registered game state for gameId: {}", gameState.getGameId());
    }


    /**
     * Get a game state by gameId
     */
    public GameState getGameStateById(String gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return null; // Return null if not found
        }
        return gameState;
    }
    
    /**
     * Get player state by game and slot
     */
    public SlotState getSlotState(String gameId, short slot) {
        GameState gameState = this.getGameStateById(gameId);
        SlotState slotState = gameState.getSlotState(slot);
        if (slotState == null) {
            log.warn("Slot state not found for gameId: {}, slot: {}", gameId, slot);
            return null; // Return null if not found
        }
        return slotState;
    }

    public Entity getEntityByStringId(String gameId, String entityId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return null;
        }

        Entity entity = gameState.getEntityByStringId(entityId);
        if (entity == null) {
            log.warn("Entity not found for gameId: {}, entityId: {}", gameId, entityId);
        }
        return entity;
    }

    public String getStringIdBySlotId(String gameId, short slot) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return null;
        }
        
        Champion champion = gameState.getChampionBySlot(slot);
        if (champion == null) {
            log.warn("Champion not found for gameId: {}, slot: {}", gameId, slot);
            return null;
        }
        
        return champion.getStringId();
    }
    
    /**
     * Update position of a slot in the game state
     */
    // public void updateSlotPosition(String gameId, short slot, Vector2 newPosition) {
    //     GameState gameState = this.getGameStateById(gameId);
    //     if (gameState != null) {
    //         gameState.setChampionPosition(slot, newPosition);
    //     }
    // }

    public void updatePosition(Entity entity, Vector2 newPosition) {
        GameState gameState = entity.getGameState();
        if (gameState == null) {
            log.warn("Game state not found for entity: {}", entity.getStringId());
            return;
        }

        gameState.setEntityPosition(entity, newPosition);// TODO


        this.updateEntityGridCellMapping(gameState, entity);
    }

    
    /**
     * Update champion health
     */
    public boolean updateChampionHealth(GameState gameState, short slot, int newCurrentHP) {

        Champion champion = gameState.getChampionBySlot(slot);

        if (champion == null) {
            log.warn("Champion not found for gameId: {}, slot: {}", gameState.getGameId(), slot);
            return false;
        }

        int oldHP = champion.getCurrentHP();
        champion.setCurrentHP(newCurrentHP);

        log.debug("Updated health for gameId: {}, slot: {} from {} to {}", 
                gameState.getGameId(), slot, oldHP, newCurrentHP);
        return true;
    }
    
    /**
     * Apply damage to a player
     */
    public boolean applyDamage(String gameId, short slot, int damage) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return false;
        }

        boolean success = this.applyDamage(gameState, slot, damage);

        if (success) {
            // Check if champion died and handle death/respawn logic
            boolean championDied = checkAndHandleChampionDeath(gameId, slot);
            if (championDied) {
                // Don't send health update if champion died - death message already sent
                // Note: Attack target clearing will be handled by the PvPService when it detects death
                return true; // Return early to prevent health update broadcast
            }
        }
        
        return success;
    }
    private boolean applyDamage(GameState gameState, short slot, int damage) {
        Champion champion = gameState.getChampionBySlot(slot);
        if (champion == null) {
            log.warn("Champion not found for gameId: {}, slot: {}", gameState.getGameId(), slot);
            return false;
        }

        int oldHP = champion.getCurrentHP();
        champion.takeDamage(damage);
        int newHP = champion.getCurrentHP();
        
        log.info("Applied {} damage to gameId: {}, slot: {} - HP: {} -> {}", 
                damage, gameState.getGameId(), slot, oldHP, newHP);

        if (!champion.isAlive()) {
            log.info("Champion in gameId: {}, slot: {} has died", gameState.getGameId(), slot);
        }
        
        return true;
    }
    
    /**
     * Heal a champion
     */
    public boolean healPlayer(String gameId, short slot, int healAmount) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return false;
        }
        
        return this.healChampion(gameState, slot, healAmount);
    }
    private boolean healChampion(GameState gameState, short slot, int healAmount) {
        Champion champion = gameState.getChampionBySlot(slot);

        if (champion == null) {
            log.warn("Champion not found for gameId: {}, slot: {}", gameState.getGameId(), slot);
            return false;
        }
        
        int oldHP = champion.getCurrentHP();
        champion.setCurrentHP(oldHP + healAmount);
        int newHP = champion.getCurrentHP();

        log.info("Healed champion in gameId: {}, slot: {} for {} HP - HP: {} -> {}", 
                gameState.getGameId(), slot, healAmount, oldHP, newHP);
        return true;
    }
    /**
     * Check if a champion has died after taking damage and handle death/respawn logic
     * @return true if champion died, false otherwise
     */
    public boolean checkAndHandleChampionDeath(String gameId, short slot) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return false;
        }

        Champion champion = gameState.getChampionBySlot(slot);
        if (champion == null || champion.getCurrentHP() > 0) {
            return false;
        }

        SlotState slotState = gameState.getSlotState(slot);
        if (slotState == null) {
            return false;
        }

        slotState.setChampionDead();
        log.info("Champion in gameId: {}, slot: {} has died", gameId,
                slot);
        
        // Clear all attack targets that were targeting this dead champion
        attackTargetingService.clearTargetsAttackingChampion(gameId, slot);
        
        // Notify clients about the death
        sendChampionDeathMessage(gameId, slot);
        scheduleChampionRespawn(gameId, slot, (short) 3);

        return true;
    }

    private void sendChampionDeathMessage(String gameId, short slot) {
        ChampionDeathSend deathMessage = new ChampionDeathSend(slot);
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(deathMessage);
            log.info("Sent champion death message for gameId: {}, slot: {}", gameId, slot);
        }
    }

    private void scheduleChampionRespawn(String gameId, short slot, short respawnTime) {
        // Create a unique key for this respawn
        String respawnKey = gameId + ":" + slot;
        
        // Check if a respawn is already scheduled for this champion
        if (activeRespawnSchedulers.containsKey(respawnKey)) {
            log.warn("Respawn already scheduled for gameId: {}, slot: {}, ignoring duplicate", gameId, slot);
            return;
        }
        
        ChampionRespawnTimeSend respawnMessage = new ChampionRespawnTimeSend(respawnTime);
        Channel playerChannel = ChannelManager.getChannelByGameIdAndSlot(gameId, slot);
        if (playerChannel != null) {
            playerChannel.writeAndFlush(respawnMessage);
            log.info("Scheduled respawn for gameId: {}, slot: {} in {} seconds", 
                    gameId, slot, respawnTime);
        } else {
            log.warn("No channel found for gameId: {}, slot: {}", gameId, slot);
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        activeRespawnSchedulers.put(respawnKey, scheduler);
        
        scheduler.schedule(() -> {
            try {
                respawnChampion(gameId, slot);
            } finally {
                // Clean up the scheduler from tracking map
                activeRespawnSchedulers.remove(respawnKey);
                scheduler.shutdown();
            }
        }, respawnTime, TimeUnit.SECONDS);
    }

    /**
     * Respawn a champion after death
     */
    private void respawnChampion(String gameId, short slot) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Cannot respawn champion, game state is null for gameId {}", gameId);
            return;
        }

        SlotState slotState = gameState.getSlotState(slot);
        if (slotState == null) {
            log.warn("Cannot respawn champion, slot state is null for gameId {}", gameId);
            return;
        }

        Vector2 initialPosition = gameState.getSpawnPosition(slot);
        Champion champion = gameState.getChampionBySlot(slot);
        int maxHealth = champion.getMaxHP();
        float rotateAngle = gameState.getSpawnRotate(slot);

        // Reset the state
        slotState.setChampionRevive();
        slotState.setCurrentHP(maxHealth);

        gameCoordinator.updatePosition(gameId, slot, initialPosition, slot, maxHealth);

        log.info("Champion in slot {} of game {} has been respawned", slot, gameId);

        //Send message
        ChampionRespawnSend respawnSend = new ChampionRespawnSend(slot, initialPosition, rotateAngle, maxHealth);
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(respawnSend);
        }
    }
    
    /**
     * Get player's current health percentage
     */
    public float getPlayerHealthPercentage(GameState gameState, short slot) {
        Champion champion = gameState.getChampionBySlot(slot);
        if (champion == null) {
            return 0.0f;
        }
        return (float) champion.getCurrentHP() / champion.getMaxHP();
    }


    public boolean updateSlotGold(GameState gameState, short slot, int newGold) {
        gameState.setGold(slot, newGold);

        log.debug("Updated gold for gameId: {}, slot: {} to {}", 
                gameState.getGameId(), slot, newGold);
        return true;
    }
    

    private void setSkillCooldownDuration(GameState gameState, short slot, float cooldown) {
        SlotState slotState = gameState.getSlotState(slot);
        if (slotState != null) {
            slotState.getChampion().setCooldown(cooldown);
            log.debug("Set skill cooldown for gameId: {}, slot: {} to {}", 
                    gameState.getGameId(), slot, cooldown);
        } else {
            log.warn("Slot state not found for gameId: {}, slot: {}", 
                    gameState.getGameId(), slot);
        }
    }


    /**
     * Clean up game state when game ends
     */
    public void cleanupGameState(String gameId) {
        GameState removed = gameStates.remove(gameId);
        if (removed != null) {
            log.info("Cleaned up game state for gameId: {} with {} players", gameId, removed.getNumPlayers());
        }
    }
    
    /**
     * Get the number of active games being tracked
     */
    public int getActiveGameCount() {
        return gameStates.size();
    }
    
    /**
     * Get the number of players in a specific game
     */
    public int getPlayerCount(String gameId) {
        GameState gameState = gameStates.get(gameId);
        return gameState != null ? gameState.getNumPlayers() : 0;
    }
    
    /**
     * Check if a game exists in the state manager
     */
    public boolean gameExists(String gameId) {
        return gameStates.containsKey(gameId);
    }
    
    /**
     * Get all player states for a game (for debugging/monitoring)
     */
    // public Map<Short, PlayerGameState> getAllPlayerStates(String gameId) {
    //     GameState gameState = gameStates.get(gameId);
    //     return gameState != null ? gameState.getAllPlayerStates() : new ConcurrentHashMap<>();
    // }
    
    /**
     * Update multiple game state attributes at once
     */
    public boolean updatePlayerGameState(String gameId, short slot, 
            Integer newHP, Integer newGold, Integer newTroops, Float newSkillCooldown) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return false;
        }


        if (newHP != null) {
            this.updateChampionHealth(gameState, slot, newHP);
        }
        if (newGold != null) {
            this.updateSlotGold(gameState, slot, newGold);
        }
        // if (newTroops != null) {
        //     playerState.setTroopCount(newTroops);
        // }
        if (newSkillCooldown != null) {
            this.setSkillCooldownDuration(gameState, slot, newSkillCooldown);
        }
        
        log.debug("Updated game state for gameId: {}, slot: {} - HP: {}, Gold: {}, Troops: {}, SkillCD: {}", 
                gameId, slot, newHP, newGold, newTroops, newSkillCooldown);
        return true;
    }
    
    /**
     * Reset player to full health (for respawn/healing abilities)
     */
    public boolean resetPlayerHealth(String gameId, short slot) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return false;
        }

        SlotState slotState = gameState.getSlotState(slot);

        
        int oldHP = slotState.getCurrentHP();

        slotState.setChampionRevive();

        log.info("Reset health for gameId: {}, slot: {} from {} to {} (max)", 
                gameId, slot, oldHP, slotState.getCurrentHP());
        return true;
    }


    public void genGold(String gameId) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return;
        }

        Integer goldGeneratedPerSecond = gameState.getGoldGeneratedPerSecond();
        for (Map.Entry<Short, SlotState> entry : gameState.getSlotStates().entrySet()) {
            SlotState slotState = entry.getValue();
            if (slotState.getChampion().isInPlayground()) {
                gameState.addGold(slotState.getSlot(), goldGeneratedPerSecond);
            }
        }
    }
    
    /**
     * Get game statistics (for monitoring and debugging)
     */
    public String getGameStatistics(String gameId) {

        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            return "Game not found: " + gameId;
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("Game Statistics for ").append(gameId).append(":\n");

        for (Map.Entry<Short, SlotState> entry : gameState.getSlotStates().entrySet()) {
            SlotState slotState = entry.getValue();
            stats.append(String.format("  Slot %d (%s): HP %d/%d, Gold: %d, Troops: %d, Alive: %s%n",
                    entry.getKey(), slotState.getChampion().getChampionEnum(),
                    slotState.getCurrentHP(), slotState.getMaxHP(),
                    slotState.getCurrentGold(), slotState.getTroopCount(), slotState.isChampionAlive()));
        }
        
        return stats.toString();
    }

    public void incrementTick(String gameId) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return;
        }

        gameState.incrementTick();
        log.debug("Incremented game tick for gameId: {}", gameId);
    }


    private GridCell getGridCellByEntity(GameState gameState, Entity entity) {
        if (gameState == null || entity == null) {
            log.warn("Invalid parameters for getting grid cell by entity");
            return null;
        }
        
        return gameState.getEntity2Grid().get(entity);
    }

    private Set<Entity> getEntitiesByGridCell(GameState gameState, GridCell gridCell) {
        if (gameState == null || gridCell == null) {
            log.warn("Invalid parameters for getting entities by grid cell");
            return null;
        }
        
        return gameState.getGrid2Entity().get(gridCell);
    }

    private void updateEntity2Grid(GameState gameState, Entity entity, GridCell gridCell) {
        if (gameState == null || entity == null || gridCell == null) {
            log.warn("Invalid parameters for updating entity to grid cell mapping");
            return;
        }
        
        gameState.getEntity2Grid().put(entity, gridCell);
        log.debug("Updated entity {} to grid cell {} in gameId: {}", entity, gridCell, gameState.getGameId());
    }

    private void popEntityFromGridCell(GameState gameState, Entity entity, GridCell gridCell) {
        if (gameState == null || entity == null || gridCell == null) {
            log.warn("Invalid parameters for popping entity from grid cell");
            return;
        }
        Set<Entity> entities = this.getEntitiesByGridCell(gameState, gridCell);
        if (entities != null) {
            entities.remove(entity);
            log.debug("Removed entity {} from grid cell {} in gameId: {}", entity, gridCell, gameState.getGameId());
        } else {
            log.warn("No entities found in grid cell {} for gameId: {}", gridCell, gameState.getGameId());
        }
        if (entities == null || entities.isEmpty()) {
            gameState.getGrid2Entity().remove(gridCell);
            log.debug("Removed empty grid cell {} from gameId: {}", gridCell, gameState.getGameId());
        }
    }

    private void pushEntityToGridCell(GameState gameState, GridCell gridCell, Entity entity) {
        if (gameState == null || gridCell == null || entity == null) {
            log.warn("Invalid parameters for pushing entity to grid cell");
            return;
        }
        
        gameState.getGrid2Entity().computeIfAbsent(gridCell, k -> ConcurrentHashMap.newKeySet()).add(entity);
        log.debug("Pushed entity {} to grid cell {} in gameId: {}", entity, gridCell, gameState.getGameId());
    }


    private void updateGridCell2Entity(GameState gameState, GridCell gridCell, Entity entity) {
        if (gameState == null || gridCell == null || entity == null) {
            log.warn("Invalid parameters for updating grid cell to entity mapping");
            return;
        }
        
        GridCell oldGridCell = this.getGridCellByEntity(gameState, entity);

        if (oldGridCell != null) { // Entity was already in map at old grid cell
            if (oldGridCell.equals(gridCell)) {
                log.debug("Entity {} is already in grid cell {}, no update needed", entity, gridCell);
                return; // No change needed
            }
            
            // Overwrite the entity's old grid cell by the new one
            this.updateEntity2Grid(gameState, entity, gridCell);

            // Remove entity in old grid cell's set
            this.popEntityFromGridCell(gameState, entity, oldGridCell);
        }

        // Update the entity to the new grid cell
        this.pushEntityToGridCell(gameState, oldGridCell, entity);
        
        log.debug("Updated entity {} from old grid cell {} to new grid cell {}",
                entity, oldGridCell, gridCell);

        log.debug("Updated grid cell {} to entity {} in gameId: {}", gridCell, entity, gameState.getGameId());
    }


    /**
     * Update the entity-grid cell mapping for an entity in the game state
     * @param gameState The game state to update
     * @param entity The entity to update
     */
    public void updateEntityGridCellMapping(GameState gameState, Entity entity) {
        if (gameState == null || entity == null) {
            log.warn("Invalid parameters for updating entity grid cell mapping");
            return;
        }

        Vector2 newPosition = entity.getCurrentPosition();
        GridCell gridCell = gameState.toGridCell(newPosition);
        
        // Update the grid cell for the entity
        this.updateGridCell2Entity(gameState, gridCell, entity);
    }


    public Set<Entity> getEntitiesInScope(GameState gameState, Shape scope) {
        
        int dir[][] = Util.EIGHT_DIRECTIONS;

        HashSet<GridCell> visitedCells = new HashSet<>();
        
        // Use BFS to find all GridCells that inside the ShapeInterface scope
        Set<Entity> entitiesInScope = ConcurrentHashMap.newKeySet();
        
        Queue<GridCell> cellsToCheck = new ConcurrentLinkedQueue<>();
        GridCell initialCell = gameState.toGridCell(scope.getCenter());
        cellsToCheck.offer(initialCell);
        visitedCells.add(initialCell);

        while(!cellsToCheck.isEmpty()) {
            GridCell currentCell = cellsToCheck.poll();
            if (currentCell == null) {
                continue;
            }


            Set<Entity> entities = this.getEntitiesByGridCell(gameState, currentCell);
            if (entities != null) {
                entitiesInScope.addAll(entities);
            }

            Vector2 cellCenter = gameState.toPosition(currentCell);
            if (scope.contains(cellCenter)) {
                // Add all entities in this cell to the result set
                
            }

            // Check neighboring cells
            for (int[] direction : dir) {
                GridCell neighborCell = currentCell.add(direction[0], direction[1]);
                
                if (!gameState.isValidGridCell(neighborCell) || 
                    visitedCells.contains(neighborCell) ||
                    !scope.contains(gameState.toPosition(neighborCell))) {
                    continue; // Skip invalid grid cells, already visited cells, or cells outside the scope
                }
                
                cellsToCheck.offer(neighborCell);
                visitedCells.add(neighborCell);
            }
        }
    
        return entitiesInScope;
    }

    public Set<Entity> getEnemiesInScope(GameState gameState, Shape scope, short slot) {
        Set<Entity> res = this.getEntitiesInScope(gameState, scope);
        for (Entity entity : res) {
            if (entity.getOwnerSlot() == slot) {
                res.remove(entity);
            }
        }
        return res;
    }

    public Set<Entity> getAlliesInScope(GameState gameState, Shape scope, short slot) {
        Set<Entity> res = this.getEntitiesInScope(gameState, scope);
        for (Entity entity : res) {
            if (entity.getOwnerSlot() != slot) {
                res.remove(entity);
            }
        }
        return res;
    }

    public Set<SkillReceivable> getSkillReceivableEnemiesInScope(GameState gameState, Shape scope, short slot) {
        Set<Entity> entities = this.getEnemiesInScope(gameState, scope, slot);
        return entities.stream()
            .filter(entity -> entity instanceof SkillReceivable)
            .map(SkillReceivable.class::cast)
            .collect(Collectors.toSet());
    }
}
