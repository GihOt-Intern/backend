package com.server.game.service.gameState;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.model.game.GameState;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameCoordinator {
    
    GameStateService gameStateService;
    
    // Store all currently active GameStates
    private final Map<String, GameState> gameStates = new ConcurrentHashMap<>();

    
    /**
     * Đăng ký game với GameState (for legacy compatibility)
     */
    public boolean registerGame(GameState gameState) {
        gameStates.put(gameState.getGameId(), gameState);
        log.info("Registered game with all schedulers: {}", gameState.getGameId());
        return true;
    }
    
    /**
     * Hủy đăng ký game khỏi cả hai schedulers và cleanup
     */
    public void unregisterGame(String gameId) {
        gameStateService.cleanupGameState(gameId);
        // pvpService.cleanupGameCooldowns(gameId); // Clean up attack cooldowns
        gameStates.remove(gameId); // Remove GameState (model)
        log.info("Unregistered game from all schedulers and cleaned up game state: {}", gameId);
    }

    public int getActiveGameCount() {
        return this.gameStates.size();
    }

    
    /**
     * Get all game IDs currently tracked by this coordinator
     */
    public Set<String> getAllGameIds() {
        return new HashSet<>(gameStates.keySet());
    }

    public Set<GameState> getAllActiveGameStates() {
        return new HashSet<>(gameStates.values());
    }

    
    /**
     * Get GameState (model) for game map/champion data access
     * Used by services that need map/champion information
     */
    public GameState getGameState(String gameId) {
        return gameStates.get(gameId);
    }

    public GameState popGameState(String gameId) {
        return gameStates.remove(gameId);
    }
}
