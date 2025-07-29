package com.server.game.service.gameState;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.game.map.component.Vector2;
import com.server.game.model.gameState.GameState;
import com.server.game.service.position.PositionService;
import com.server.game.service.pvp.PvPService;
import com.server.game.service.scheduler.BroadcastScheduler;
import com.server.game.service.scheduler.GameLogicScheduler;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameCoordinator {
    
    @Autowired
    private BroadcastScheduler broadcastScheduler;
    
    @Autowired
    private GameLogicScheduler gameLogicScheduler;
    
    @Autowired
    private PositionService positionService;
    
    @Autowired
    private GameStateService gameStateService;
    
    @Autowired
    private PvPService pvpService;
    
    // Store GameState (model) for game map/champion data access
    private final Map<String, GameState> gameStates = new ConcurrentHashMap<>();
    
    /**
     * Đăng ký game với cả hai schedulers và lưu GameState
     */
    public void registerGame(String gameId) {
        broadcastScheduler.registerGame(gameId);
        gameLogicScheduler.registerGame(gameId);
        log.info("Registered game with all schedulers: {}", gameId);
    }
    
    /**
     * Đăng ký game với GameState (for legacy compatibility)
     */
    public void registerGame(String gameId, GameState gameState) {
        gameStates.put(gameId, gameState);
        registerGame(gameId);
    }
    
    /**
     * Hủy đăng ký game khỏi cả hai schedulers và cleanup
     */
    public void unregisterGame(String gameId) {
        broadcastScheduler.unregisterGame(gameId);
        gameLogicScheduler.unregisterGame(gameId);
        gameStateService.cleanupGameState(gameId);
        pvpService.cleanupGameCooldowns(gameId); // Clean up attack cooldowns
        gameStates.remove(gameId); // Remove GameState (model)
        log.info("Unregistered game from all schedulers and cleaned up game state: {}", gameId);
    }

    /**
     * Kiểm tra xem game có hoạt động hay không (trong cả hai schedulers)
     */
    public boolean isGameActive(String gameId) {
        return broadcastScheduler.isGameActive(gameId) && gameLogicScheduler.isGameActive(gameId);
    }
    
    /**
     * Get all game IDs currently tracked by this coordinator
     */
    public Set<String> getAllGameIds() {
        return new HashSet<>(gameStates.keySet());
    }

    /**
     * Cập nhật vị trí của người chơi
     */
    public void updatePosition(String gameId, short slot, Vector2 position, float speed, long timestamp) {
        if (!isGameActive(gameId)) {
            log.warn("Attempted to update position for inactive game: {}", gameId);
            return;
        }
        
        // Update position in the PositionService
        positionService.updatePosition(gameId, slot, position, speed, timestamp);
        log.debug("Updated position for gameId: {}, slot: {}, position: {}", gameId, slot, position);
    }
    
    /**
     * Get GameState (model) for game map/champion data access
     * Used by services that need map/champion information
     */
    public GameState getGameState(String gameId) {
        if (!isGameActive(gameId)) {
            log.warn("Attempted to get state for inactive game: {}", gameId);
            return null;
        }
        return gameStates.get(gameId);
    }
    
    /**
     * Get game statistics across both schedulers
     */
    public GameStats getGameStats() {
        return GameStats.builder()
                .activeBroadcastGames(broadcastScheduler.getActiveGameCount())
                .activeLogicGames(gameLogicScheduler.getActiveGameCount())
                .totalActiveGames(Math.max(broadcastScheduler.getActiveGameCount(), gameLogicScheduler.getActiveGameCount()))
                .build();
    }
    
    /**
     * Inner class for game statistics
     */
    public static class GameStats {
        private final int activeBroadcastGames;
        private final int activeLogicGames;
        private final int totalActiveGames;
        
        private GameStats(int activeBroadcastGames, int activeLogicGames, int totalActiveGames) {
            this.activeBroadcastGames = activeBroadcastGames;
            this.activeLogicGames = activeLogicGames;
            this.totalActiveGames = totalActiveGames;
        }
        
        public static GameStatsBuilder builder() {
            return new GameStatsBuilder();
        }
        
        public int getActiveBroadcastGames() { return activeBroadcastGames; }
        public int getActiveLogicGames() { return activeLogicGames; }
        public int getTotalActiveGames() { return totalActiveGames; }
        
        public static class GameStatsBuilder {
            private int activeBroadcastGames;
            private int activeLogicGames;
            private int totalActiveGames;
            
            public GameStatsBuilder activeBroadcastGames(int activeBroadcastGames) {
                this.activeBroadcastGames = activeBroadcastGames;
                return this;
            }
            
            public GameStatsBuilder activeLogicGames(int activeLogicGames) {
                this.activeLogicGames = activeLogicGames;
                return this;
            }
            
            public GameStatsBuilder totalActiveGames(int totalActiveGames) {
                this.totalActiveGames = totalActiveGames;
                return this;
            }
            
            public GameStats build() {
                return new GameStats(activeBroadcastGames, activeLogicGames, totalActiveGames);
            }
        }
    }
}
