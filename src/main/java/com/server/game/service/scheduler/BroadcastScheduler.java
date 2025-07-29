package com.server.game.service.scheduler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.server.game.service.position.PositionBroadcastService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BroadcastScheduler {
    
    @Autowired
    private PositionBroadcastService positionBroadcastService;
    
    // Lưu trữ các game đang hoạt động cho broadcasting
    private final Set<String> activeGames = ConcurrentHashMap.newKeySet();
    
    /**
     * Đăng ký game để thực hiện broadcasting
     */
    public void registerGame(String gameId) {
        activeGames.add(gameId);
        log.info("Registered game for broadcasting: {}", gameId);
    }
    
    /**
     * Hủy đăng ký game khỏi broadcasting
     */
    public void unregisterGame(String gameId) {
        activeGames.remove(gameId);
        // Notify broadcast service to clean up
        positionBroadcastService.unregisterGame(gameId);
        log.info("Unregistered game from broadcasting: {}", gameId);
    }

    /**
     * Kiểm tra xem game có hoạt động hay không
     */
    public boolean isGameActive(String gameId) {
        return activeGames.contains(gameId);
    }
    
    /**
     * High-frequency broadcasting loop - runs every 50ms (20 FPS)
     * Handles position updates broadcasting to clients
     */
    @Scheduled(fixedDelay = 33) // 33ms = 30 FPS
    public void broadcastLoop() {
        for (String gameId : activeGames) {
            try {
                // Broadcast position updates to all players in the game
                positionBroadcastService.broadcastGamePositions(gameId);
                
            } catch (Exception e) {
                log.error("Error in broadcast loop for game: {}", gameId, e);
            }
        }
    }
    
    /**
     * Get all active games being broadcasted
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
