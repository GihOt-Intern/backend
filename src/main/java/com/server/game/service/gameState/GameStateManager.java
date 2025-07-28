package com.server.game.service.gameState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.game.service.GameStateService;
import com.server.game.util.ChampionEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * Advanced Game State Manager that provides high-level operations
 * for managing game state across multiple games and players.
 * This class acts as a facade for complex game state operations.
 */
@Slf4j
@Component
public class GameStateManager {
    
    @Autowired
    private GameStateService gameStateService;
    
    // Cache for game-wide statistics
    private final Map<String, GameStatistics> gameStatisticsCache = new ConcurrentHashMap<>();
    
    /**
     * Initialize a complete game with all players
     */
    public boolean initializeGame(String gameId, Map<Short, ChampionEnum> slotToChampionMap, 
            Map<ChampionEnum, Integer> championInitialHPMap) {
        
        log.info("Initializing game state for gameId: {} with {} players", gameId, slotToChampionMap.size());
        
        // Initialize game state
        gameStateService.initializeGameState(gameId);
        
        // Initialize each player
        for (Map.Entry<Short, ChampionEnum> entry : slotToChampionMap.entrySet()) {
            Short slot = entry.getKey();
            ChampionEnum championId = entry.getValue();
            Integer initialHP = championInitialHPMap.get(championId);
            
            if (initialHP != null) {
                gameStateService.initializePlayerState(gameId, slot, championId, initialHP);
                log.debug("Initialized player state for slot {} with champion {} and HP {}", 
                        slot, championId, initialHP);
            } else {
                log.warn("No initial HP found for champion: {}", championId);
                return false;
            }
        }
        
        // Initialize game statistics
        initializeGameStatistics(gameId);
        
        log.info("Successfully initialized game state for gameId: {}", gameId);
        return true;
    }
    
    /**
     * Process damage with advanced features (armor, resistances, etc.)
     */
    public boolean processAdvancedDamage(String gameId, short targetSlot, int damage, 
            boolean isMagicalDamage, boolean canCrit, float critChance) {
        
        PlayerGameState playerState = gameStateService.getPlayerState(gameId, targetSlot);
        if (playerState == null) {
            log.warn("Player state not found for gameId: {}, slot: {}", gameId, targetSlot);
            return false;
        }
        
        // Check if player is invulnerable
        if (playerState.isCurrentlyInvulnerable()) {
            log.debug("Player slot {} is invulnerable, damage ignored", targetSlot);
            return false;
        }
        
        // Calculate critical hit
        int finalDamage = damage;
        if (canCrit && Math.random() < critChance) {
            finalDamage = (int) (damage * 1.5f); // 150% damage on crit
            log.debug("Critical hit! Damage increased from {} to {}", damage, finalDamage);
        }
        
        // Apply advanced damage calculation
        playerState.takeAdvancedDamage(finalDamage, isMagicalDamage);
        
        // Update game statistics
        updateDamageStatistics(gameId, targetSlot, finalDamage);
        
        log.debug("Applied {} damage to slot {} in game {} (magical: {}, crit: {})", 
                finalDamage, targetSlot, gameId, isMagicalDamage, finalDamage > damage);
        
        return true;
    }
    
    /**
     * Process healing with limits and effects
     */
    public boolean processAdvancedHealing(String gameId, short targetSlot, int healAmount, 
            boolean isInstant, float healingModifier) {
        
        PlayerGameState playerState = gameStateService.getPlayerState(gameId, targetSlot);
        if (playerState == null || !playerState.isAlive()) {
            return false;
        }
        
        int finalHealAmount = (int) (healAmount * healingModifier);
        
        if (isInstant) {
            playerState.heal(finalHealAmount);
        } else {
            // Gradual healing could be implemented here
            playerState.heal(finalHealAmount);
        }
        
        // Update game statistics
        updateHealingStatistics(gameId, targetSlot, finalHealAmount);
        
        log.debug("Applied {} healing to slot {} in game {} (modifier: {})", 
                finalHealAmount, targetSlot, gameId, healingModifier);
        
        return true;
    }
    
    /**
     * Get players who are alive in a game
     */
    public List<Short> getAlivePlayers(String gameId) {
        Map<Short, PlayerGameState> gameState = gameStateService.getGameState(gameId);
        return gameState.entrySet().stream()
                .filter(entry -> entry.getValue().isAlive())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Get players who are dead in a game
     */
    public List<Short> getDeadPlayers(String gameId) {
        Map<Short, PlayerGameState> gameState = gameStateService.getGameState(gameId);
        return gameState.entrySet().stream()
                .filter(entry -> !entry.getValue().isAlive())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Get players with low health (below threshold)
     */
    public List<Short> getLowHealthPlayers(String gameId, float healthThreshold) {
        Map<Short, PlayerGameState> gameState = gameStateService.getGameState(gameId);
        return gameState.entrySet().stream()
                .filter(entry -> {
                    PlayerGameState player = entry.getValue();
                    return player.isAlive() && player.getHealthPercentage() < healthThreshold;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if game has ended (only one player alive, or all dead)
     */
    public boolean isGameEnded(String gameId) {
        List<Short> alivePlayers = getAlivePlayers(gameId);
        return alivePlayers.size() <= 1;
    }
    
    /**
     * Get game winner (returns null if game not ended or tie)
     */
    public Short getGameWinner(String gameId) {
        List<Short> alivePlayers = getAlivePlayers(gameId);
        return alivePlayers.size() == 1 ? alivePlayers.get(0) : null;
    }
    
    /**
     * Apply temporary invulnerability to a player
     */
    public boolean applyInvulnerability(String gameId, short slot, long durationMs) {
        PlayerGameState playerState = gameStateService.getPlayerState(gameId, slot);
        if (playerState == null) {
            return false;
        }
        
        playerState.applyInvulnerability(durationMs);
        log.info("Applied {}ms invulnerability to slot {} in game {}", durationMs, slot, gameId);
        return true;
    }
    
    /**
     * Respawn a dead player with specified health percentage
     */
    public boolean respawnPlayer(String gameId, short slot, float healthPercentage, long invulnerabilityMs) {
        PlayerGameState playerState = gameStateService.getPlayerState(gameId, slot);
        if (playerState == null || playerState.isAlive()) {
            return false;
        }
        
        // Restore health
        int respawnHP = (int) (playerState.getMaxHP() * healthPercentage);
        playerState.setCurrentHP(respawnHP);
        playerState.setDead(false);
        
        // Apply temporary invulnerability
        if (invulnerabilityMs > 0) {
            playerState.applyInvulnerability(invulnerabilityMs);
        }
        
        // Update last action time
        playerState.updateLastActionTime();
        
        log.info("Respawned player slot {} in game {} with {}% health and {}ms invulnerability", 
                slot, gameId, (int)(healthPercentage * 100), invulnerabilityMs);
        
        return true;
    }
    
    /**
     * Get comprehensive game state summary
     */
    public GameStateSnapshot getGameStateSnapshot(String gameId) {
        Map<Short, PlayerGameState> gameState = gameStateService.getGameState(gameId);
        GameStatistics stats = gameStatisticsCache.get(gameId);
        
        return new GameStateSnapshot(gameId, gameState, stats, 
                getAlivePlayers(gameId), getDeadPlayers(gameId));
    }
    
    /**
     * Clean up game state and statistics
     */
    public void cleanupGame(String gameId) {
        gameStateService.cleanupGameState(gameId);
        gameStatisticsCache.remove(gameId);
        log.info("Cleaned up complete game state for gameId: {}", gameId);
    }
    
    // Private helper methods
    
    private void initializeGameStatistics(String gameId) {
        gameStatisticsCache.put(gameId, new GameStatistics(gameId));
    }
    
    private void updateDamageStatistics(String gameId, short slot, int damage) {
        GameStatistics stats = gameStatisticsCache.get(gameId);
        if (stats != null) {
            stats.addDamageDealt(slot, damage);
        }
    }
    
    private void updateHealingStatistics(String gameId, short slot, int healing) {
        GameStatistics stats = gameStatisticsCache.get(gameId);
        if (stats != null) {
            stats.addHealingDone(slot, healing);
        }
    }
    
    // Inner classes for data structures
    
    public static class GameStateSnapshot {
        public final String gameId;
        public final Map<Short, PlayerGameState> playerStates;
        public final GameStatistics statistics;
        public final List<Short> alivePlayers;
        public final List<Short> deadPlayers;
        public final long timestamp;
        
        public GameStateSnapshot(String gameId, Map<Short, PlayerGameState> playerStates, 
                GameStatistics statistics, List<Short> alivePlayers, List<Short> deadPlayers) {
            this.gameId = gameId;
            this.playerStates = playerStates;
            this.statistics = statistics;
            this.alivePlayers = alivePlayers;
            this.deadPlayers = deadPlayers;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static class GameStatistics {
        public final String gameId;
        public final Map<Short, Integer> damageDealt = new ConcurrentHashMap<>();
        public final Map<Short, Integer> healingDone = new ConcurrentHashMap<>();
        public final Map<Short, Integer> damageReceived = new ConcurrentHashMap<>();
        public final long gameStartTime;
        
        public GameStatistics(String gameId) {
            this.gameId = gameId;
            this.gameStartTime = System.currentTimeMillis();
        }
        
        public void addDamageDealt(short slot, int damage) {
            damageDealt.merge(slot, damage, Integer::sum);
        }
        
        public void addHealingDone(short slot, int healing) {
            healingDone.merge(slot, healing, Integer::sum);
        }
        
        public void addDamageReceived(short slot, int damage) {
            damageReceived.merge(slot, damage, Integer::sum);
        }
        
        public long getGameDuration() {
            return System.currentTimeMillis() - gameStartTime;
        }
    }
}
