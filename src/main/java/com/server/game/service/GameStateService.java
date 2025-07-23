package com.server.game.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.service.gameState.PlayerGameState;
import com.server.game.util.ChampionEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GameStateService {
    
    // gameId -> (slot -> PlayerGameState)
    private final Map<String, Map<Short, PlayerGameState>> gameStates = new ConcurrentHashMap<>();
    
    /**
     * Initialize game state for a specific game
     */
    public void initializeGameState(String gameId) {
        gameStates.putIfAbsent(gameId, new ConcurrentHashMap<>());
        log.info("Initialized game state for gameId: {}", gameId);
    }
    
    /**
     * Initialize player state with champion data
     */
    public void initializePlayerState(String gameId, short slot, ChampionEnum championId, int initialHP) {
        Map<Short, PlayerGameState> gameState = gameStates.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        
        PlayerGameState playerState = new PlayerGameState(slot, championId, initialHP);
        gameState.put(slot, playerState);
        
        log.info("Initialized player state for gameId: {}, slot: {}, championId: {}, initialHP: {}", 
                gameId, slot, championId, initialHP);
    }
    
    /**
     * Get player state by game and slot
     */
    public PlayerGameState getPlayerState(String gameId, short slot) {
        Map<Short, PlayerGameState> gameState = gameStates.get(gameId);
        if (gameState == null) {
            log.warn("Game state not found for gameId: {}", gameId);
            return null;
        }
        return gameState.get(slot);
    }
    
    /**
     * Get all player states in a game
     */
    public Map<Short, PlayerGameState> getGameState(String gameId) {
        return gameStates.getOrDefault(gameId, new ConcurrentHashMap<>());
    }
    
    /**
     * Update player health
     */
    public boolean updatePlayerHealth(String gameId, short slot, int newCurrentHP) {
        PlayerGameState playerState = getPlayerState(gameId, slot);
        if (playerState == null) {
            log.warn("Player state not found for gameId: {}, slot: {}", gameId, slot);
            return false;
        }
        
        int oldHP = playerState.getCurrentHP();
        playerState.setCurrentHP(newCurrentHP);
        
        log.debug("Updated health for gameId: {}, slot: {} from {} to {}", 
                gameId, slot, oldHP, newCurrentHP);
        return true;
    }
    
    /**
     * Apply damage to a player
     */
    public boolean applyDamage(String gameId, short slot, int damage) {
        PlayerGameState playerState = getPlayerState(gameId, slot);
        if (playerState == null) {
            log.warn("Player state not found for gameId: {}, slot: {}", gameId, slot);
            return false;
        }
        
        int oldHP = playerState.getCurrentHP();
        playerState.takeDamage(damage);
        int newHP = playerState.getCurrentHP();
        
        log.info("Applied {} damage to gameId: {}, slot: {} - HP: {} -> {}", 
                damage, gameId, slot, oldHP, newHP);
        
        if (!playerState.isAlive()) {
            log.info("Player in gameId: {}, slot: {} has died", gameId, slot);
        }
        
        return true;
    }
    
    /**
     * Heal a player
     */
    public boolean healPlayer(String gameId, short slot, int healAmount) {
        PlayerGameState playerState = getPlayerState(gameId, slot);
        if (playerState == null) {
            log.warn("Player state not found for gameId: {}, slot: {}", gameId, slot);
            return false;
        }
        
        int oldHP = playerState.getCurrentHP();
        playerState.heal(healAmount);
        int newHP = playerState.getCurrentHP();
        
        log.info("Healed player in gameId: {}, slot: {} for {} HP - HP: {} -> {}", 
                gameId, slot, healAmount, oldHP, newHP);
        return true;
    }
    
    /**
     * Check if a player is alive
     */
    public boolean isPlayerAlive(String gameId, short slot) {
        PlayerGameState playerState = getPlayerState(gameId, slot);
        return playerState != null && playerState.isAlive();
    }
    
    /**
     * Get player's current health percentage
     */
    public float getPlayerHealthPercentage(String gameId, short slot) {
        PlayerGameState playerState = getPlayerState(gameId, slot);
        if (playerState == null) {
            return 0.0f;
        }
        return (float) playerState.getCurrentHP() / playerState.getMaxHP();
    }
    
    /**
     * Clean up game state when game ends
     */
    public void cleanupGameState(String gameId) {
        Map<Short, PlayerGameState> removed = gameStates.remove(gameId);
        if (removed != null) {
            log.info("Cleaned up game state for gameId: {} with {} players", gameId, removed.size());
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
        Map<Short, PlayerGameState> gameState = gameStates.get(gameId);
        return gameState != null ? gameState.size() : 0;
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
    public Map<Short, PlayerGameState> getAllPlayerStates(String gameId) {
        return gameStates.getOrDefault(gameId, new ConcurrentHashMap<>());
    }
    
    /**
     * Update multiple game state attributes at once
     */
    public boolean updatePlayerGameState(String gameId, short slot, 
            Integer newHP, Integer newGold, Integer newTroops, Float newSkillCooldown) {
        PlayerGameState playerState = getPlayerState(gameId, slot);
        if (playerState == null) {
            log.warn("Player state not found for gameId: {}, slot: {}", gameId, slot);
            return false;
        }
        
        if (newHP != null) {
            playerState.setCurrentHP(newHP);
        }
        if (newGold != null) {
            playerState.setGold(newGold);
        }
        if (newTroops != null) {
            playerState.setTroopCount(newTroops);
        }
        if (newSkillCooldown != null) {
            playerState.setSkillCooldownDuration(newSkillCooldown);
        }
        
        log.debug("Updated game state for gameId: {}, slot: {} - HP: {}, Gold: {}, Troops: {}, SkillCD: {}", 
                gameId, slot, playerState.getCurrentHP(), playerState.getGold(), 
                playerState.getTroopCount(), playerState.getSkillCooldownDuration());
        return true;
    }
    
    /**
     * Reset player to full health (for respawn/healing abilities)
     */
    public boolean resetPlayerHealth(String gameId, short slot) {
        PlayerGameState playerState = getPlayerState(gameId, slot);
        if (playerState == null) {
            log.warn("Player state not found for gameId: {}, slot: {}", gameId, slot);
            return false;
        }
        
        int oldHP = playerState.getCurrentHP();
        playerState.setCurrentHP(playerState.getMaxHP());
        playerState.setDead(false);
        
        log.info("Reset health for gameId: {}, slot: {} from {} to {}", 
                gameId, slot, oldHP, playerState.getMaxHP());
        return true;
    }
    
    /**
     * Get game statistics (for monitoring and debugging)
     */
    public String getGameStatistics(String gameId) {
        Map<Short, PlayerGameState> gameState = gameStates.get(gameId);
        if (gameState == null) {
            return "Game not found: " + gameId;
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("Game Statistics for ").append(gameId).append(":\n");
        
        for (Map.Entry<Short, PlayerGameState> entry : gameState.entrySet()) {
            PlayerGameState player = entry.getValue();
            stats.append(String.format("  Slot %d (%s): HP %d/%d, Gold: %d, Troops: %d, Alive: %s%n",
                    entry.getKey(), player.getChampionId(),
                    player.getCurrentHP(), player.getMaxHP(),
                    player.getGold(), player.getTroopCount(), player.isAlive()));
        }
        
        return stats.toString();
    }
}
