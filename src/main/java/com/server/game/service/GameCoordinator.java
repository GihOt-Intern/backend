package com.server.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.game.map.component.Vector2;

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
    
    /**
     * Đăng ký game với cả hai schedulers
     */
    public void registerGame(String gameId) {
        broadcastScheduler.registerGame(gameId);
        gameLogicScheduler.registerGame(gameId);
        log.info("Registered game with all schedulers: {}", gameId);
    }
    
    /**
     * Hủy đăng ký game khỏi cả hai schedulers
     */
    public void unregisterGame(String gameId) {
        broadcastScheduler.unregisterGame(gameId);
        gameLogicScheduler.unregisterGame(gameId);
        gameStateService.cleanupGameState(gameId);
        log.info("Unregistered game from all schedulers and cleaned up game state: {}", gameId);
    }

    /**
     * Kiểm tra xem game có hoạt động hay không (trong cả hai schedulers)
     */
    public boolean isGameActive(String gameId) {
        return broadcastScheduler.isGameActive(gameId) && gameLogicScheduler.isGameActive(gameId);
    }

    /**
     * Cập nhật vị trí của người chơi
     */
    public void updatePosition(String gameId, short slot, Vector2 position, long timestamp) {
        if (!isGameActive(gameId)) {
            log.warn("Attempted to update position for inactive game: {}", gameId);
            return;
        }
        
        // Update position in the PositionService
        positionService.updatePosition(gameId, slot, position, timestamp);
        log.debug("Updated position for gameId: {}, slot: {}, position: {}", gameId, slot, position);
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
