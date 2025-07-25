package com.server.game.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.HeartbeatMessage;
import com.server.game.service.gameState.HealthRegenerationService;
import com.server.game.service.troop.TroopAI;
import com.server.game.service.troop.TroopInstance;
import com.server.game.service.troop.TroopManager;
import com.server.game.util.TroopEnum;

import io.netty.channel.Channel;
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

    @Autowired
    private TroopManager troopManager;

    @Autowired
    private TroopAI troopAI;
    
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
    @Scheduled(fixedDelay = 16) // 16ms = 62.5 FPS for responsive gameplay
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
                processTroopMovement(gameId);
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
                processTroopAI(gameId);
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

    /**
     * Process troop movement for all troops in the game
     */
    private void processTroopMovement(String gameId) {
        Collection<TroopInstance> troops = troopManager.getGameTroops(gameId);
        if (troops.isEmpty()) {
            return;
        }

        for (TroopInstance troop : troops) {
            if (troop.isAlive() && troop.isMoving() && troop.getTargetPosition() != null) {
                float deltaTime = 0.2f;
                float moveSpeed = getTroopMoveSpeed(troop.getTroopType());

                troop.moveTowards(troop.getTargetPosition(), moveSpeed, deltaTime);
            }
            troop.updateEffects();
        }
    }

    /**
     * Process troop AI
     */
    private void processTroopAI(String gameId) {
        troopAI.processGameAI(gameId);
    }

    /**
     * Get the movement speed for a troop type
     */
    private float getTroopMoveSpeed(TroopEnum troopType) {
        return switch (troopType) {
            case CROSSBAWL -> 1.5f;  // Slower ranged unit
            case AXIS -> 2.5f;       // Fast melee unit
            case SHADOW -> 3.0f;     // Very fast assassin
            case HEALER -> 1.8f;     // Medium speed support
            default -> 2.0f;         // Default speed
        };
    }
}
