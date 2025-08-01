package com.server.game.service.scheduler;

import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.server.game.model.game.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.HeartbeatMessage;
import com.server.game.service.attack.AttackService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.goldGeneration.GoldGenerationService;
import com.server.game.service.move.MoveService;
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
    AttackService attackService;
    // TroopManager troopManager;
    GoldGenerationService goldGenerationService;
    GameStateService gameStateService;
    
    /**
     * Main game logic loop - runs every 33ms (~30 FPS)
     * Handles movement updates and combat logic
     */
    @Scheduled(fixedDelayString = "${game.tick-interval-ms}") // 33ms ~ 30 FPS for responsive gameplay
    public void gameLogicLoop() {
        for (GameState gameState : gameStateService.getAllActiveGameStates()) {
            try {

                // Update game tick
                gameStateService.incrementTick(gameState.getGameId());


                // Update movement positions
                moveService.updatePositions(gameState);
                // troopManager.updateTroopMovements(gameState.getGameId(), 0.05f);
                
                // Process attack targeting and continuous combat
                attackService.processAttacks(gameState);

                
                // TODO: Add other high-frequency game systems here
                // - Spell/ability cooldowns
                // - Game state validation
                // - Collision detection
                
            } catch (Exception e) {
                log.error("Error in game logic loop for game: {}", gameState.getGameId(), e);
            }
        }
    }


    /**
     * Handles gold auto-generation when slot is in playground,
     * update every 1000ms (1 second)
     */
    @Scheduled(fixedDelay = 1000) // 1000ms = 1 FPS for gold generation
    public void goldGenerationLoop() {
        for (GameState gameState : gameStateService.getAllActiveGameStates()) {
            try {
                goldGenerationService.generateGold(gameState.getGameId());

            } catch (Exception e) {
                log.error("Error in game logic loop for game: {}", gameState.getGameId(), e);
            }
        }
    }




    
    /**
     * Slower game logic loop - runs every 200ms (5 FPS)
     * Handles less critical game systems
     */
    @Scheduled(fixedDelay = 200) // 200ms = 5 FPS for non-critical systems
    public void slowGameLogicLoop() {
        for (GameState gameState : gameStateService.getAllActiveGameStates()) {
            try {
                // TODO: Add slower update systems here
                // - Resource generation
                // - AI decision making
                // - Game statistics updates
                // - Health regeneration
                // - Status effect updates
                
            } catch (Exception e) {
                log.error("Error in slow game logic loop for game: {}", gameState.getGameId(), e);
            }
        }
    }
    
    /**
     * Very slow game logic loop - runs every 1000ms (1 FPS)
     * Handles background game systems
     */
    @Scheduled(fixedDelay = 1000) // 1000ms = 1 FPS for background systems
    public void backgroundGameLogicLoop() {
        for (GameState gameState : gameStateService.getAllActiveGameStates()) {
            try {
                // TODO: Add background systems here
                // - Game session cleanup
                // - Performance metrics collection
                // - Anti-cheat validation
                // - Database persistence
                
            } catch (Exception e) {
                log.error("Error in background game logic loop for game: {}", gameState.getGameId(), e);
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
}
