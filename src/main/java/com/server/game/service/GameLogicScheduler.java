package com.server.game.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.server.game.service.gameState.HealthRegenerationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameLogicScheduler {
    
    @Autowired
    private MoveService moveService;
    
    @Autowired
    private AttackTargetingService attackTargetingService;
    
    @Autowired
    private HealthRegenerationService healthRegenerationService;
    
    // Lưu trữ các game đang hoạt động cho game logic
    private final Set<String> activeGames = ConcurrentHashMap.newKeySet();
    
    /**
     * Đăng ký game để thực hiện game logic updates
     */
    public void registerGame(String gameId) {
        activeGames.add(gameId);
        log.info("Registered game for game logic: {}", gameId);
    }
    
    /**
     * Hủy đăng ký game khỏi game logic
     */
    public void unregisterGame(String gameId) {
        activeGames.remove(gameId);
        log.info("Unregistered game from game logic: {}", gameId);
    }

    /**
     * Kiểm tra xem game có hoạt động hay không
     */
    public boolean isGameActive(String gameId) {
        return activeGames.contains(gameId);
    }
    
    /**
     * Main game logic loop - runs every 50ms (20 FPS)
     * Handles movement updates and combat logic
     */
    @Scheduled(fixedDelay = 50) // 50ms = 20 FPS for responsive gameplay
    public void gameLogicLoop() {
        for (String gameId : activeGames) {
            try {
                // Update movement positions
                moveService.updatePositions(gameId);
                
                // Process attack targeting and continuous combat
                attackTargetingService.processAllAttackers(gameId);
                
                // TODO: Add other high-frequency game systems here
                // - Spell/ability cooldowns
                // - Game state validation
                // - Collision detection
                
            } catch (Exception e) {
                log.error("Error in game logic loop for game: {}", gameId, e);
            }
        }
    }
    
    /**
     * Slower game logic loop - runs every 200ms (5 FPS)
     * Handles less critical game systems
     */
    @Scheduled(fixedDelay = 200) // 200ms = 5 FPS for non-critical systems
    public void slowGameLogicLoop() {
        for (String gameId : activeGames) {
            try {
                // TODO: Add slower update systems here
                // - Troop spawning
                // - Resource generation
                // - AI decision making
                // - Game statistics updates
                // - Health regeneration
                // - Status effect updates
                
            } catch (Exception e) {
                log.error("Error in slow game logic loop for game: {}", gameId, e);
            }
        }
    }
    
    /**
     * Very slow game logic loop - runs every 1000ms (1 FPS)
     * Handles background game systems
     */
    @Scheduled(fixedDelay = 1000) // 1000ms = 1 FPS for background systems
    public void backgroundGameLogicLoop() {
        for (String gameId : activeGames) {
            try {
                // TODO: Add background systems here
                // - Game session cleanup
                // - Performance metrics collection
                // - Anti-cheat validation
                // - Database persistence
                
            } catch (Exception e) {
                log.error("Error in background game logic loop for game: {}", gameId, e);
            }
        }
    }
    
    /**
     * Get all active games in game logic
     */
    public Set<String> getActiveGames() {
        return Set.copyOf(activeGames);
    }
    
    /**
     * Get the number of active games
     */
    public int getActiveGameCount() {
        return activeGames.size();
    }
}
