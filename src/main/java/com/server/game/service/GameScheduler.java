package com.server.game.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.server.game.map.component.Vector2;
// import com.server.game.netty.receiveMessageHandler.GameHandler.GameState;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameScheduler {
    
    @Autowired
    private PositionBroadcastService positionBroadcastService;
    
    @Autowired
    private MoveService moveService;

    @Autowired
    private PositionService positionService;
    
    // Lưu trữ các game đang hoạt động
    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    
    public static class GameState {}; // TODO


    /**
     * Đăng ký game để thực hiện các task định kỳ
     */
    public void registerGame(String gameId, GameState gameState) {
        activeGames.put(gameId, gameState);
        log.info("Registered game for scheduling: {}", gameId);
    }
    
    /**
     * Hủy đăng ký game
     */
    public void unregisterGame(String gameId) {
        if (!this.isGameActive(gameId)) {
            log.warn("Attempted to unregister non-existent game: {}", gameId);
            return;
        }
        activeGames.remove(gameId);
        // Notify services to clean up game data
        positionBroadcastService.unregisterGame(gameId);
        log.info("Unregistered game from scheduling: {}", gameId);
    }

    /**
     * Kiểm tra xem game có hoạt động hay không
     */
    public boolean isGameActive(String gameId) {
        return activeGames.containsKey(gameId);
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
        log.info("Updated position for gameId: {}, slot: {}, position: {}, speed: {}", gameId, slot, position, speed);
    }

    /**
     * Main game loop - runs every 33ms (~30 FPS)
     * Handles all game-related periodic tasks
     */
    @Scheduled(fixedDelay = 33) // 33ms = ~30 times per second = ~30 fps
    public void gameLoop() {
        for (String gameId : activeGames.keySet()) {
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
        for (String gameId : activeGames.keySet()) {
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
