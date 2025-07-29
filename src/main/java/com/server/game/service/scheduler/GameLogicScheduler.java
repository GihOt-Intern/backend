package com.server.game.service.scheduler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.HeartbeatMessage;
import com.server.game.service.attack.AttackTargetingService;
import com.server.game.service.goldGeneration.GoldGenerationService;
import com.server.game.service.move.MoveService;
// import com.server.game.service.gameState.HealthRegenerationService;
import com.server.game.service.troop.TroopManager;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GameLogicScheduler {

    MoveService moveService;
    AttackTargetingService attackTargetingService;
    TroopManager troopManager;
    GoldGenerationService goldGenerationService;
    

    
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
     * Main game logic loop - runs every 33ms (~30 FPS)
     * Handles movement updates and combat logic
     */
    @Scheduled(fixedDelay = 33) // 33ms ~ 30 FPS for responsive gameplay
    public void gameLogicLoop() {
        for (String gameId : activeGames) {
            try {
                // Update movement positions
                moveService.updatePositions(gameId);
                troopManager.updateTroopMovements(gameId, 0.05f);
                
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
     * Handles gold auto-generation when slot is in playground,
     * update every 1000ms (1 second)
     */
    @Scheduled(fixedDelay = 1000) // 1000ms = 1 FPS for gold generation
    public void goldGenerationLoop() {
        for (String gameId : activeGames) {
            try {
                goldGenerationService.generateGold(gameId);

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
     * Heartbeat method to keep the game logic scheduler alive
     * @return
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    public void sendHeartbeats() {
        for (Map.Entry<String, Channel> entry : ChannelManager.getAllUserChannels().entrySet()) {
            String userId = entry.getKey();
            Channel channel = entry.getValue();

            if (channel.isActive()) {
                channel.writeAndFlush(new HeartbeatMessage())
                    .addListener(future -> {
                        if (!future.isSuccess()) {
                            System.out.println(">>> Heartbeat failed for user " + userId + ". Cleaning up channel.");
                            ChannelManager.unregister(channel);
                        }
                });
            } else {
                System.out.println(">>> Inactive channel for user " + userId + ". Removing from manager.");
                ChannelManager.unregister(channel);
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
