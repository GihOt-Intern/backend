package com.server.game.service.gameState;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.building.Tower;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.model.game.entityIface.HasFixedPosition;
import com.server.game.model.game.entityIface.SkillReceivable;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.map.shape.Shape;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageHandler.AnimationMessageHandler;
import com.server.game.netty.messageHandler.GameStateMessageHandler;
import com.server.game.netty.messageHandler.PlaygroundMessageHandler;
import com.server.game.netty.sendObject.GameOverSend;
import com.server.game.netty.sendObject.LoserSend;
import com.server.game.netty.sendObject.entity.EntitiesRemovedSend;
import com.server.game.netty.sendObject.entity.EntityDeathSend;
import com.server.game.netty.sendObject.respawn.ChampionRespawnSend;
import com.server.game.netty.sendObject.respawn.ChampionRespawnTimeSend;
import com.server.game.service.move.MoveService;
import com.server.game.util.Util;
import com.server.game.model.game.Entity;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class GameStateService {

    private final GameCoordinator gameCoordinator;
    private final PlaygroundMessageHandler playgroundMessageHandler;
    private final AnimationMessageHandler animationMessageHandler;
    private final GameStateMessageHandler gameStateMessageHandler;
    private final MoveService moveService;

    // Track active respawn schedulers to prevent duplicates: gameId:slot -> scheduler
    private final Map<String, ScheduledExecutorService> activeRespawnSchedulers = new ConcurrentHashMap<>();

    
    public GameState getGameStateById(String gameId) {
        GameState gameState = gameCoordinator.getGameState(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return null; // Return null if not found
        }
        return gameState;
    }

    public Set<GameState> getAllActiveGameStates() {
        return gameCoordinator.getAllActiveGameStates();
    }

    public void addEntityTo(GameState gameState, Entity entity) {
        if (gameState == null || entity == null) {
            log.warn("Invalid parameters for adding entity to game state");
            return;
        }
        
        gameState.addEntity(entity);
        log.debug("Added entity {} to game state {}", entity.getStringId(), gameState.getGameId());
    }
    
    /** 
     * Remove an entity from the game state
     */
    public void removeEntity(GameState gameState, Entity entity) {
        if (entity == null || entity.getStringId() == null) {
            log.warn("Invalid entity or stringId for gameId: {}", gameState.getGameId());
            return;
        }

        gameState.removeEntity(entity);
    }
    

    public Entity getEntityByStringId(String gameId, String entityId) {
        GameState gameState = gameCoordinator.getGameState(gameId);
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
    
    public Entity getEntityByStringId(GameState gameState, String entityId) {
        Entity entity = gameState.getEntityByStringId(entityId);
        if (entity == null) {
            log.warn("Entity not found for gameId: {}, entityId: {}", gameState.getGameId(), entityId);
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

    public void sendTowerDeathMessage(String gameId, Tower tower) {
        String towerId = tower.getStringId();
        if (towerId == null) {
            log.warn("Could not find tower ID for gameId: {}", gameId);
            return;
        }

        EntityDeathSend deathMessage = new EntityDeathSend(towerId);
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(deathMessage);
            //log.info("Sent tower death message for gameId: {}, towerId: {}", gameId, towerId);
        } else {
            log.warn("No channel found for gameId: {} when sending tower death message", gameId);
        }
    }

    public void sendEntitiesRemoved(String gameId, List<String> removedEntityIds, long timestamp) {
        EntitiesRemovedSend entitiesRemovedSend = new EntitiesRemovedSend(removedEntityIds, timestamp);
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            log.info("Sending remove entities");
            channel.writeAndFlush(entitiesRemovedSend);
            // log.info("Sent entities removed message for gameId: {}", gameId);
        } else {
            log.warn("No channel found for gameId: {} when sending entities removed message", gameId);
        }
    }

    public void sendGameOver(String gameId, short winnerSlot, short loserSlot, long timestamp, String burgId) {
        EntityDeathSend entityDeathSend = new EntityDeathSend(burgId);
        GameOverSend gameOverSend = new GameOverSend(winnerSlot);
        LoserSend loserSend = new LoserSend();
        Channel channel = ChannelManager.getChannelByGameIdAndSlot(gameId, winnerSlot);
        if (channel != null) {
            log.info("Sending game over message for gameId: {}, winnerSlot: {}", gameId, winnerSlot);
            channel.writeAndFlush(gameOverSend);
            channel.writeAndFlush(entityDeathSend);
            // log.info("Sent game over message for gameId: {}, winnerSlot: {}", gameId, winnerSlot);
        } else {
            log.warn("No channel found for gameId: {} when sending game over message", gameId);
        }
        Channel loserChannel = ChannelManager.getChannelByGameIdAndSlot(gameId, loserSlot);
        if (loserChannel != null) {
            log.info("Sending loser message for gameId: {}, loserSlot: {}", gameId, loserSlot);
            loserChannel.writeAndFlush(loserSend);
        } else {
            log.warn("No channel found for gameId: {} when sending loser message", gameId);
        }
        gameCoordinator.unregisterGame(gameId);
    }

    public void scheduleChampionRespawn(SlotState slotState, short respawnTime) {

        String gameId = slotState.getGameState().getGameId();
        short slot = slotState.getSlot();

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
            // log.info("Scheduled respawn for gameId: {}, slot: {} in {} seconds", 
            //         gameId, slot, respawnTime);
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
        float rotateAngle = gameState.getSpawnRotate(slotState);

        // Reset the state
        slotState.setChampionRevive();
        champion.setCurrentHP(maxHealth);
        champion.setCurrentPosition(initialPosition);

        //Send message
        ChampionRespawnSend respawnSend = new ChampionRespawnSend(
            slotState.getChampion().getStringId(), 
            initialPosition, rotateAngle, maxHealth);
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(respawnSend);
        }
    }

    /**
     * Clean up game state when game ends
     */
    public void cleanupGameState(String gameId) {
        GameState removed = gameCoordinator.popGameState(gameId);
        if (removed != null) {
            // log.info("Cleaned up game state for gameId: {} with {} players", gameId, removed.getNumPlayers());
        }
    }
    
    /**
     * Get the number of active games being tracked
     */
    public int getActiveGameCount() {
        return gameCoordinator.getActiveGameCount();
    }
    
    /**
     * Get the number of players in a specific game
     */
    public int getPlayerCount(String gameId) {
        GameState gameState = gameCoordinator.getGameState(gameId);
        return gameState != null ? gameState.getNumPlayers() : 0;
    }
    
    /**
     * Check if a game exists in the state manager
     */
    public boolean gameExists(String gameId) {
        return gameCoordinator.getGameState(gameId) != null;
    }
    

    public void autoIncreaseGold4SlotsInPlayground(GameState gameState) {
        if (gameState == null) {
            log.warn("Game state is null");
            return;
        }

        Integer goldAutoIncreasePerSecond = gameState.getGoldGeneratedPerSecond();
        for (Map.Entry<Short, SlotState> entry : gameState.getSlotStates().entrySet()) {
            SlotState slotState = entry.getValue();
            if (slotState.getChampion().isInPlayground()) {
                gameState.increaseGoldFor(slotState, goldAutoIncreasePerSecond);
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

    public void incrementTick(GameState gameState) {
        if (gameState == null) {
            return;
        }

        gameState.incrementTick();
    }

    public Set<Entity> getEntitiesByGridCell(GameState gameState, GridCell gridCell) {
        if (gameState == null || gridCell == null) {
            // log.info("Invalid parameters for getting entities by grid cell");
            return null;
        }
        
        return gameState.getGrid2Entity().get(gridCell);
    }

    // called before update Entity's current position 
    public void removeEntityFromGridCellMapping(Entity entity) {
        if (entity == null) {
            log.warn("Invalid entity for removing from grid cell mapping");
            return;
        }

        GridCell oldGridCell = entity.getCurrentGridCell();
        if (oldGridCell == null) {
            log.warn("Entity {} has no current grid cell, cannot remove from mapping", entity.getStringId());
            return;
        }

        GameState gameState = entity.getGameState();
        gameState.getGrid2Entity().computeIfPresent(oldGridCell, (cell, entities) -> {
            entities.remove(entity);
            // // log.info("Removed entity {} from grid cell {}", entity.getStringId(), cell);

            if (entities.isEmpty()) {
                gameState.getGrid2Entity().remove(cell);
                // // log.info("Removed empty grid cell {} from gameId: {}", cell, gameState.getGameId());
            }
            return entities; // no need
        });
    }


    /**
     * Update the entity-grid cell mapping for an entity in the game state
     * Called after the entity's current position is updated
     * @param gameState The game state to update
     * @param entity The entity to update
     */
    public void addEntityToGridCellMapping(Entity entity) {
        if (entity == null) {
            log.warn("Invalid parameters for updating entity grid cell mapping");
            return;
        }

        GridCell newGridCell = entity.getCurrentGridCell();

        // // log.info("Updating entity, new position: {} to grid cell {}", 
        //         // entity.getCurrentPosition(), newGridCell);
        
        GameState gameState = entity.getGameState();
        gameState.getGrid2Entity().computeIfAbsent(newGridCell, k -> ConcurrentHashMap.newKeySet()).add(entity);        
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

    public Set<Entity> getEnemiesInScope(GameState gameState, Shape scope, SlotState slotState) {
        Set<Entity> res = this.getEntitiesInScope(gameState, scope);
        for (Entity entity : res) {
            if (slotState.equals(entity.getOwnerSlot())) {
                res.remove(entity);
            }
        }
        return res;
    }

    public Set<Entity> getAlliesInScope(GameState gameState, Shape scope, SlotState slotState) {
        Set<Entity> res = this.getEntitiesInScope(gameState, scope);
        for (Entity entity : res) {
            if (!slotState.equals(entity.getOwnerSlot())) {
                res.remove(entity);
            }
        }
        return res;
    }

    public Set<SkillReceivable> getSkillReceivableEnemiesInScope(GameState gameState, Shape scope, SlotState slotState) {
        Set<Entity> entities = this.getEnemiesInScope(gameState, scope, slotState);
        return entities.stream()
            .filter(entity -> entity instanceof SkillReceivable)
            .map(SkillReceivable.class::cast)
            .collect(Collectors.toSet());
    }

    public void stopChampionsAttackingTo(GameState gameState, Entity entity) {
        Set<Champion> champions = gameState.getChampions();
        for (Champion champion : champions) {
            Entity attackTarget = champion.getAttackTarget();
            if (entity.equals(attackTarget)) {
                this.setStopAttacking(champion);
            }
        }
    }

    public void setMove(Entity mover, Vector2 toPosition) {
        if (mover instanceof HasFixedPosition) {
            log.debug("Cannot set move for entity: {}", mover.getStringId());
            return;
        }
        moveService.setMove(mover, toPosition, false);
    }
    
    public void setStopMoving(Entity mover, boolean isForced) {
        if (mover instanceof HasFixedPosition) {
            log.debug("Cannot stop move for entity: {}", mover.getStringId());
            return;
        }
        mover.setStopMoving(isForced);
    }

    public void setStopAttacking(Entity attacker) {
        attacker.setStopAttacking();
    }


    public void setChampionDead(Champion champion) {
        champion.getOwnerSlot().setChampionDead();
    }

    public void handleStolingGold(Champion victim, Entity killer) {
        Integer stolenGold = Math.round(victim.getCurrentGold()*0.3f);
        victim.decreaseGold(stolenGold);
        killer.increaseGold(stolenGold);
    }


    public void sendPositionUpdate(GameState gameState, Entity mover) {
        this.gameStateMessageHandler.sendPositionUpdate(gameState, mover);
    }

    public void sendInPlaygroundUpdateMessage(GameState gameState, SlotState slot, boolean isInPlayground) {
        if (gameState == null || slot == null) {
            log.warn("Invalid parameters for sending in-playground update message");
            return;
        }
        
        // Send the update message to all players in the game
        this.playgroundMessageHandler.sendInPlaygroundUpdateMessage(
            gameState.getGameId(), slot.getSlot(), isInPlayground);
    }

    public void sendAttackAnimation(AttackContext ctx) {
        this.animationMessageHandler.sendAttackAnimation(ctx);
    }

    public void sendCastSkillAnimation(CastSkillContext ctx) {
        this.animationMessageHandler.sendCastSkillAnimation(ctx);
    }

    public void sendHealthUpdate(String gameId, Entity target, int actualDamage, long timestamp) {
        this.gameStateMessageHandler.sendHealthUpdate(gameId, target, actualDamage, timestamp);
    }

    public void sendGoldChangeMessage(String gameId, short slot, int newGold) {
        this.playgroundMessageHandler.sendGoldChangeMessage(gameId, slot, newGold);
    }

    public void sendGoldMineSpawnMessage(String gameId, String goldMineId, 
        boolean isSmallGoldMine, Vector2 position, int initHP) {
        
            this.playgroundMessageHandler
            .sendGoldMineSpawnMessage(gameId, goldMineId, isSmallGoldMine, position, initHP);
    }
    
    public void sendEntityDeathMessage(GameState gameState, String entityId) {
        this.gameStateMessageHandler.sendEntityDeath(gameState.getGameId(), entityId);
    }
}
