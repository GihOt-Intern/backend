package com.server.game.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameScheduler {
    
    @Autowired
    private PositionBroadcastService positionBroadcastService;
    
    @Autowired
    private MoveService moveService;
    
    // Lưu trữ các game đang hoạt động
    private final Set<String> activeGames = ConcurrentHashMap.newKeySet();
    
    /**
     * Đăng ký game để thực hiện các task định kỳ
     */
    public void registerGame(String gameId) {
        activeGames.add(gameId);
        log.info("Registered game for scheduling: {}", gameId);
    }
    
    /**
     * Hủy đăng ký game
     */
    public void unregisterGame(String gameId) {
        activeGames.remove(gameId);
        // Notify services to clean up game data
        positionBroadcastService.unregisterGame(gameId);
        log.info("Unregistered game from scheduling: {}", gameId);
    }

    /**
     * Kiểm tra xem game có hoạt động hay không
     */
    public boolean isGameActive(String gameId) {
        return activeGames.contains(gameId);
    }
    
    /**
     * Main game loop - runs every 50ms (20 FPS)
     * Handles all game-related periodic tasks
     */
    @Scheduled(fixedDelay = 50) // 50ms = 20 times per second = 20 fps
    public void gameLoop() {
        for (String gameId : activeGames) {  
            try {
                // Update movement positions
                moveService.updatePositions(gameId);
                
                // Broadcast position updates
                positionBroadcastService.broadcastGamePositions(gameId);
                
                // TODO: Add other game systems here
                // - Combat/Attack system updates
                // - Troop AI updates
                // - Resource management
                // - Spell/ability cooldowns
                // - Game state validation
                
            } catch (Exception e) {
                log.error("Error in game loop for game: {}", gameId, e);
            }
        }
    }
    
    // TODO: Add other scheduled methods for different game systems
    
    /**
     * Slower update cycle for less critical systems (e.g., 5 FPS)
     */
    @Scheduled(fixedDelay = 200) // 200ms = 5 times per second
    public void slowGameLoop() {
        for (String gameId : activeGames) {
            try {
                // TODO: Add slower update systems here
                // - Troop spawning
                // - Resource generation
                // - AI decision making
                // - Game statistics updates
                
            } catch (Exception e) {
                log.error("Error in slow game loop for game: {}", gameId, e);
            }
        }
    }
}
