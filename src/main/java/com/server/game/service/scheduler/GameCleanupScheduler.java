package com.server.game.service.scheduler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.server.game.model.game.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.troop.TroopManager;


import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for cleaning up games that have no active channels
 * after a specified timeout period (30 seconds).
 */
@Slf4j
@Service
public class GameCleanupScheduler {
    
    @Autowired
    private GameCoordinator gameCoordinator;

    @Autowired
    private TroopManager troopManager;
    
    // Track when games first become empty (no channels)
    private final Map<GameState, Long> emptyGameTimestamps = new ConcurrentHashMap<>();
    
    // Timeout in milliseconds (30 seconds)
    private static final long CLEANUP_TIMEOUT_MS = 30_000;
    
    // Counter for cleanup operations (for monitoring)
    private volatile int totalCleanupsPerformed = 0;
    
    /**
     * Scheduled task that runs every 10 seconds to check for empty games
     * and clean them up after the timeout period.
     */
    @Scheduled(fixedDelay = 10_000) // Check every 10 seconds
    public void cleanupEmptyGames() {
        long currentTime = System.currentTimeMillis();
        
        // Get all games that are currently tracked by GameCoordinator
        Set<GameState> activeGameStates = gameCoordinator.getAllActiveGameStates();

        if (activeGameStates.isEmpty()) {
            return; // No games to check
        }

        log.debug("Checking {} games for empty channels", activeGameStates.size());

        for (GameState gameState : activeGameStates) {
            Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameState.getGameId());
            
            if (gameChannels == null || gameChannels.isEmpty()) {
                // Game has no channels
                handleEmptyGame(gameState, currentTime);
            } else {
                // Game has channels, remove from empty tracking if it was there
                if (emptyGameTimestamps.remove(gameState) != null) {
                    log.debug("Game {} now has active channels, removed from cleanup tracking", gameState.getGameId());
                }
            }
        }
        
        // Clean up tracking for games that no longer exist
        emptyGameTimestamps.keySet().removeIf(emptyGameState -> {
            if (!activeGameStates.contains(emptyGameState)) {
                log.debug("Game {} no longer exists, removing from cleanup tracking", emptyGameState.getGameId());
                return true;
            }
            return false;
        });
        
        // Log summary if there are games being tracked for cleanup
        if (!emptyGameTimestamps.isEmpty()) {
            log.debug("Currently tracking {} empty games for cleanup", emptyGameTimestamps.size());
        }
    }
    
    /**
     * Handle a game that has no active channels
     */
    private void handleEmptyGame(GameState gameState, long currentTime) {
        Long emptyStartTime = emptyGameTimestamps.get(gameState);
        
        if (emptyStartTime == null) {
            // First time we detected this game as empty
            emptyGameTimestamps.put(gameState, currentTime);
            log.debug("Game {} detected as empty, starting cleanup timer", gameState.getGameId());
        } else {
            // Check if enough time has passed
            long elapsedTime = currentTime - emptyStartTime;
            if (elapsedTime >= CLEANUP_TIMEOUT_MS) {
                // Time to clean up this game
                this.cleanupGame(gameState);
                emptyGameTimestamps.remove(gameState);
            } else {
                long remainingTime = CLEANUP_TIMEOUT_MS - elapsedTime;
                log.debug("Game {} will be cleaned up in {}ms", gameState.getGameId(), remainingTime);
            }
        }
    }
    
    /**
     * Clean up a game that has been empty for too long
     */
    private void cleanupGame(GameState gameState) {
        try {
            log.info("Cleaning up empty game after {} seconds: {}", CLEANUP_TIMEOUT_MS / 1000, gameState.getGameId());

            // Unregister the game from all schedulers and clean up resources
            gameCoordinator.unregisterGame(gameState.getGameId());
            troopManager.cleanupGameTroops(gameState.getGameId());
            

            // Increment cleanup counter
            totalCleanupsPerformed++;

            log.info("Successfully cleaned up empty game: {} (total cleanups: {})", gameState.getGameId(), totalCleanupsPerformed);
        } catch (Exception e) {
            log.error("Error cleaning up game: {}", gameState.getGameId(), e);
            // Remove from tracking even if cleanup failed to prevent repeated attempts
            emptyGameTimestamps.remove(gameState);
        }
    }
    
    /**
     * Get current cleanup statistics for monitoring
     */
    public CleanupStats getCleanupStats() {
        long currentTime = System.currentTimeMillis();
        int gamesNearCleanup = 0;
        
        for (Long emptyStartTime : emptyGameTimestamps.values()) {
            long elapsedTime = currentTime - emptyStartTime;
            if (elapsedTime >= CLEANUP_TIMEOUT_MS - 5000) { // Within 5 seconds of cleanup
                gamesNearCleanup++;
            }
        }
        
        return new CleanupStats(
            emptyGameTimestamps.size(),
            gamesNearCleanup,
            CLEANUP_TIMEOUT_MS / 1000,
            totalCleanupsPerformed
        );
    }
    
    /**
     * Manually trigger cleanup check (for testing/administrative purposes)
     */
    public void manualCleanupCheck() {
        log.info("Manual cleanup check triggered");
        cleanupEmptyGames();
    }
    
    /**
     * Force cleanup of a specific game (for administrative purposes)
     */
    public boolean forceCleanupGame(GameState gameState) {
        try {
            Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameState.getGameId());
            if (gameChannels != null && !gameChannels.isEmpty()) {
                log.warn("Force cleanup requested for game {} but it still has {} active channels",
                        gameState.getGameId(), gameChannels.size());
                return false;
            }

            log.info("Force cleaning up game: {}", gameState.getGameId());
            cleanupGame(gameState);
            return true;
        } catch (Exception e) {
            log.error("Error in force cleanup for game: {}", gameState.getGameId(), e);
            return false;
        }
    }
    
    /**
     * Notify that a game has become empty (called when last channel disconnects)
     * This starts the cleanup timer for the game.
     */
    public void notifyGameEmpty(String gameId) {
        GameState gameState = gameCoordinator.getGameState(gameId);
        long currentTime = System.currentTimeMillis();
        
        // Only start tracking if not already tracking
        if (!emptyGameTimestamps.containsKey(gameState)) {
            emptyGameTimestamps.put(gameState, currentTime);
            log.info("Game {} became empty, starting {} second cleanup timer",
                    gameState.getGameId(), CLEANUP_TIMEOUT_MS / 1000);
        }
    }
    
    /**
     * Periodic status logging for monitoring (runs every 5 minutes)
     */
    @Scheduled(fixedDelay = 300_000) // Every 5 minutes
    public void logCleanupStatus() {
        CleanupStats stats = getCleanupStats();
        if (stats.getGamesBeingTracked() > 0 || stats.getTotalCleanupsPerformed() > 0) {
            log.info("Game cleanup status: {}", stats);
        }
    }
    
    /**
     * Statistics about the cleanup service
     */
    public static class CleanupStats {
        private final int gamesBeingTracked;
        private final int gamesNearCleanup;
        private final long timeoutSeconds;
        private final int totalCleanupsPerformed;
        
        public CleanupStats(int gamesBeingTracked, int gamesNearCleanup, long timeoutSeconds, int totalCleanupsPerformed) {
            this.gamesBeingTracked = gamesBeingTracked;
            this.gamesNearCleanup = gamesNearCleanup;
            this.timeoutSeconds = timeoutSeconds;
            this.totalCleanupsPerformed = totalCleanupsPerformed;
        }
        
        public int getGamesBeingTracked() { return gamesBeingTracked; }
        public int getGamesNearCleanup() { return gamesNearCleanup; }
        public long getTimeoutSeconds() { return timeoutSeconds; }
        public int getTotalCleanupsPerformed() { return totalCleanupsPerformed; }
        
        @Override
        public String toString() {
            return String.format("CleanupStats{gamesBeingTracked=%d, gamesNearCleanup=%d, timeoutSeconds=%d, totalCleanups=%d}", 
                               gamesBeingTracked, gamesNearCleanup, timeoutSeconds, totalCleanupsPerformed);
        }
    }
}
