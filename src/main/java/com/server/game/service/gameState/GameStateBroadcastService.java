package com.server.game.service.gameState;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.game.model.gameState.GameState;
import com.server.game.model.gameState.SlotState;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.pvp.HealthUpdateSend;

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
        
        GameState gameState = gameStateService.getGameStateById(gameId);
        
        int currentHealth = gameState.getCurrentHP(targetSlot);
        int maxHealth = gameState.getMaxHP(targetSlot);
        
        HealthUpdateSend healthUpdate = new HealthUpdateSend(targetSlot, currentHealth, maxHealth, damage, timestamp);
        
        // Broadcast to all players in the game
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(healthUpdate);
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
        
        GameState gameState = gameStateService.getGameStateById(gameId);
        
        int currentHealth = gameState.getCurrentHP(targetSlot);
        int maxHealth = gameState.getMaxHP(targetSlot);
        
        // Negative damage indicates healing
        HealthUpdateSend healthUpdate = new HealthUpdateSend(targetSlot, currentHealth, maxHealth, -healAmount, timestamp);
        
        // Broadcast to all players in the game
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(healthUpdate);
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
        GameState gameState = gameStateService.getGameStateById(gameId);

        int currentHealth = gameState.getCurrentHP(slot);
        int maxHealth = gameState.getMaxHP(slot);
        

        // Send health update showing full heal (respawn)
        HealthUpdateSend healthUpdate = new HealthUpdateSend(slot, currentHealth, maxHealth, 0, System.currentTimeMillis());
        
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(healthUpdate);
            log.info("Broadcasted respawn for slot {} in game {} - HP: {}/{} (invulnerability: {}ms)", 
                    slot, gameId, currentHealth, maxHealth, invulnerabilityDuration);
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
        GameState gameState = gameStateService.getGameStateById(gameId);
        long timestamp = System.currentTimeMillis();
        
        Map<Short, SlotState> slotStates = gameState.getSlotStates();


        for (Map.Entry<Short, SlotState> entry : slotStates.entrySet()) {
            Short slot = entry.getKey();
            SlotState slotState = entry.getValue();

            HealthUpdateSend healthUpdate = new HealthUpdateSend(
                slot, 
                slotState.getCurrentHP(), 
                slotState.getMaxHP(), 
                0, // No damage, just status update
                timestamp
            );
            
            Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
            if (channel != null) {
                channel.writeAndFlush(healthUpdate);
                log.info("Broadcasted health status for slot {} in game {} - HP: {}/{}", 
                        slot, gameId, slotState.getCurrentHP(), slotState.getMaxHP());
            }
        }
        
        log.debug("Broadcasted health status for all players in game {}", gameId);
    }
}
