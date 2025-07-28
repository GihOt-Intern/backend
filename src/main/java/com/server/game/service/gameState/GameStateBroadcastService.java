package com.server.game.service.gameState;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.pvp.HealthUpdateSend;
import com.server.game.service.GameStateService;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for broadcasting game state updates to players
 */
@Slf4j
@Service
public class GameStateBroadcastService {
    
    @Autowired
    private GameStateService gameStateService;
    
    /**
     * Broadcast health update to all players in a game
     */
    public void broadcastHealthUpdate(String gameId, short targetSlot, int damage, long timestamp) {
        log.debug("Broadcasting health update for slot {} with damage {}", targetSlot, damage);
        
        // Apply damage using GameStateService
        boolean success = gameStateService.applyDamage(gameId, targetSlot, damage);
        if (!success) {
            log.warn("Failed to apply damage to player slot {} in game {}", targetSlot, gameId);
            return;
        }
        
        // Get actual health values from game state
        var playerState = gameStateService.getPlayerState(gameId, targetSlot);
        if (playerState == null) {
            log.warn("Player state not found for slot {} in game {}", targetSlot, gameId);
            return;
        }
        
        int currentHealth = playerState.getCurrentHP();
        int maxHealth = playerState.getMaxHP();
        
        HealthUpdateSend healthUpdate = new HealthUpdateSend(targetSlot, currentHealth, maxHealth, damage, timestamp);
        
        // Broadcast to all players in the game
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(healthUpdate);
                }
            });
            
            log.info("Broadcasted health update for slot {} in game {} - HP: {}/{} (damage: {})", 
                    targetSlot, gameId, currentHealth, maxHealth, damage);
        }
    }
    
    /**
     * Broadcast healing update to all players in a game
     */
    public void broadcastHealingUpdate(String gameId, short targetSlot, int healAmount, long timestamp) {
        log.debug("Broadcasting healing update for slot {} with heal amount {}", targetSlot, healAmount);
        
        // Apply healing using GameStateService
        boolean success = gameStateService.healPlayer(gameId, targetSlot, healAmount);
        if (!success) {
            log.warn("Failed to heal player slot {} in game {}", targetSlot, gameId);
            return;
        }
        
        // Get actual health values from game state
        var playerState = gameStateService.getPlayerState(gameId, targetSlot);
        if (playerState == null) {
            log.warn("Player state not found for slot {} in game {}", targetSlot, gameId);
            return;
        }
        
        int currentHealth = playerState.getCurrentHP();
        int maxHealth = playerState.getMaxHP();
        
        // Negative damage indicates healing
        HealthUpdateSend healthUpdate = new HealthUpdateSend(targetSlot, currentHealth, maxHealth, -healAmount, timestamp);
        
        // Broadcast to all players in the game
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(healthUpdate);
                }
            });
            
            log.info("Broadcasted healing update for slot {} in game {} - HP: {}/{} (heal: {})", 
                    targetSlot, gameId, currentHealth, maxHealth, healAmount);
        }
    }
    
    /**
     * Broadcast game state update to all players (for major events)
     */
    public void broadcastGameStateUpdate(String gameId, String updateType, Map<String, Object> data) {
        // This can be extended to send custom game state updates
        // For now, we'll just log the update
        log.info("Game state update for {}: {} - {}", gameId, updateType, data);
    }
    
    /**
     * Broadcast player respawn event
     */
    public void broadcastPlayerRespawn(String gameId, short slot, long invulnerabilityDuration) {
        var playerState = gameStateService.getPlayerState(gameId, slot);
        if (playerState == null) {
            return;
        }
        
        int currentHealth = playerState.getCurrentHP();
        int maxHealth = playerState.getMaxHP();
        
        // Send health update showing full heal (respawn)
        HealthUpdateSend healthUpdate = new HealthUpdateSend(slot, currentHealth, maxHealth, 0, System.currentTimeMillis());
        
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(healthUpdate);
                }
            });
            
            log.info("Broadcasted respawn for slot {} in game {} with {}ms invulnerability", 
                    slot, gameId, invulnerabilityDuration);
        }
    }
    
    /**
     * Broadcast multiple health updates at once (for AoE effects)
     */
    public void broadcastMultipleHealthUpdates(String gameId, Map<Short, Integer> slotToDamageMap, long timestamp) {
        for (Map.Entry<Short, Integer> entry : slotToDamageMap.entrySet()) {
            broadcastHealthUpdate(gameId, entry.getKey(), entry.getValue(), timestamp);
        }
    }
    
    /**
     * Get and broadcast current health status for all players
     */
    public void broadcastAllPlayerHealthStatus(String gameId) {
        Map<Short, PlayerGameState> gameState = gameStateService.getGameState(gameId);
        long timestamp = System.currentTimeMillis();
        
        for (Map.Entry<Short, PlayerGameState> entry : gameState.entrySet()) {
            Short slot = entry.getKey();
            PlayerGameState playerState = entry.getValue();
            
            HealthUpdateSend healthUpdate = new HealthUpdateSend(
                slot, 
                playerState.getCurrentHP(), 
                playerState.getMaxHP(), 
                0, // No damage, just status update
                timestamp
            );
            
            Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
            if (gameChannels != null) {
                gameChannels.forEach(channel -> {
                    if (channel != null && channel.isActive()) {
                        channel.writeAndFlush(healthUpdate);
                    }
                });
            }
        }
        
        log.debug("Broadcasted health status for all players in game {}", gameId);
    }
}
