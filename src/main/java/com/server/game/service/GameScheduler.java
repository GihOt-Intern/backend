package com.server.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.game.map.component.Vector2;
import com.server.game.model.GameState;

import lombok.extern.slf4j.Slf4j;

/**
 * @deprecated This class is deprecated and will be removed in future versions.
 * Use {@link GameCoordinator} instead for unified scheduler management.
 * 
 * This class now delegates all operations to GameCoordinator to maintain
 * backward compatibility during the migration period.
 */
@Deprecated
@Service
@Slf4j
public class GameScheduler {
    
    @Autowired
    private GameCoordinator gameCoordinator;
    
    /**
     * @deprecated Use {@link GameCoordinator#registerGame(String, GameState)} instead
     */
    @Deprecated
    public void registerGame(String gameId, GameState gameState) {
        log.warn("GameScheduler.registerGame() is deprecated. Use GameCoordinator instead.");
        gameCoordinator.registerGame(gameId, gameState);
    }
    
    /**
     * @deprecated Use {@link GameCoordinator#unregisterGame(String)} instead
     */
    @Deprecated
    public void unregisterGame(String gameId) {
        log.warn("GameScheduler.unregisterGame() is deprecated. Use GameCoordinator instead.");
        gameCoordinator.unregisterGame(gameId);
    }

    /**
     * @deprecated Use {@link GameCoordinator#isGameActive(String)} instead
     */
    @Deprecated
    public boolean isGameActive(String gameId) {
        log.warn("GameScheduler.isGameActive() is deprecated. Use GameCoordinator instead.");
        return gameCoordinator.isGameActive(gameId);
    }

    /**
     * @deprecated Use {@link GameCoordinator#getGameState(String)} instead
     */
    @Deprecated
    public GameState getGameState(String gameId) {
        log.warn("GameScheduler.getGameState() is deprecated. Use GameCoordinator instead.");
        return gameCoordinator.getGameState(gameId);
    }

    /**
     * @deprecated Use {@link GameCoordinator#updatePosition(String, short, Vector2, float, long)} instead
     */
    @Deprecated
    public void updatePosition(String gameId, short slot, Vector2 position, float speed, long timestamp) {
        log.warn("GameScheduler.updatePosition() is deprecated. Use GameCoordinator instead.");
        gameCoordinator.updatePosition(gameId, slot, position, speed, timestamp);
    }

    // NOTE: The @Scheduled methods have been removed as they are now handled by
    // BroadcastScheduler and GameLogicScheduler through GameCoordinator
}
