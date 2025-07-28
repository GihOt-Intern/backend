package com.server.game.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.map.component.Vector2;
import com.server.game.map.object.Champion;
import com.server.game.model.GameState;
import com.server.game.model.SlotState;
// import com.server.game.service.gameState.PlayerGameState;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GameStateService {
    
    // gameId -> GameState
    private final Map<String, GameState> gameStates = new ConcurrentHashMap<>();
    
    /**
     * Initialize game state for a specific game
     */
    public void register(GameState gameState) {
        gameStates.put(gameState.getGameId(), gameState);
        log.info("Registered game state for gameId: {}", gameState.getGameId());
    }
    
    /**
     * Initialize player state with champion data
     */
    // public void initializePlayerState(String gameId, short slot, ChampionEnum championId, int initialHP) {
    //     Map<Short, PlayerGameState> gameState = gameStates.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        
    //     PlayerGameState playerState = new PlayerGameState(slot, championId, initialHP);
    //     gameState.put(slot, playerState);
        
    //     log.info("Initialized player state for gameId: {}, slot: {}, championId: {}, initialHP: {}", 
    //             gameId, slot, championId, initialHP);
    // }

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
    
    /**
     * Update position of a slot in the game state
     */
    public void updateSlotPosition(String gameId, short slot, Vector2 newPosition) {
        GameState gameState = this.getGameStateById(gameId);
        if (gameState != null) {
            gameState.setSlotPosition(slot, newPosition);
        }
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
        
        return this.applyDamage(gameState, slot, damage);
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
     * Check if a player is alive
     */
    public boolean isPlayerAlive(String gameId, short slot) {
        GameState gameState = this.getGameStateById(gameId);
        Champion champion = gameState.getChampionBySlot(slot);
        
        if (champion == null) {
            log.warn("Champion not found for gameId: {}, slot: {}", gameState.getGameId(), slot);
            return false;
        }
        
        return champion.isAlive();
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
        slotState.setCurrentHP(slotState.getMaxHP());
        slotState.setDead();


        slotState.setCurrentHP(slotState.getMaxHP());
        slotState.setDead();

        log.info("Reset health for gameId: {}, slot: {} from {} to {}", 
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
            if (slotState.isInPlayGround()) {
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
                    entry.getKey(), slotState.getChampion().getId(),
                    slotState.getCurrentHP(), slotState.getMaxHP(),
                    slotState.getCurrentGold(), slotState.getTroopCount(), slotState.isAlive()));
        }
        
        return stats.toString();
    }



    
}
