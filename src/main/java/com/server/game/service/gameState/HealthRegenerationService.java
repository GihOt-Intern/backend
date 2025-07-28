package com.server.game.service.gameState;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.pvp.HealthUpdateSend;
import com.server.game.service.GameStateService;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HealthRegenerationService {
    
    @Autowired
    private GameStateService gameStateService;
    
    // Configuration constants
    private static final long NO_DAMAGE_COOLDOWN_MS = 5000; // 5 seconds before regeneration starts
    private static final int REGENERATION_AMOUNT = 10; // HP per tick
    private static final float REGENERATION_PERCENTAGE = 0.02f; // 2% of max HP per tick
    
    /**
     * Process health regeneration for all players in a game
     */
    public void processHealthRegeneration(String gameId) {
        Map<Short, PlayerGameState> gameState = gameStateService.getGameState(gameId);
        if (gameState.isEmpty()) {
            return;
        }
        
        for (PlayerGameState playerState : gameState.values()) {
            processPlayerRegeneration(gameId, playerState);
        }
    }
    
    /**
     * Process health regeneration for a specific player
     */
    private void processPlayerRegeneration(String gameId, PlayerGameState playerState) {
        // Skip if player is dead or at full health
        if (!playerState.isAlive() || playerState.getCurrentHP() >= playerState.getMaxHP()) {
            return;
        }
        
        // Check if enough time has passed since last damage
        if (!playerState.canStartRegeneration(NO_DAMAGE_COOLDOWN_MS)) {
            return;
        }
        
        // Calculate regeneration amount
        int regenAmount = Math.max(REGENERATION_AMOUNT, 
                (int) (playerState.getMaxHP() * REGENERATION_PERCENTAGE));
        
        // Apply healing
        int oldHP = playerState.getCurrentHP();
        playerState.heal(regenAmount);
        int newHP = playerState.getCurrentHP();
        
        // Only broadcast if health actually changed
        if (newHP > oldHP) {
            playerState.setRegenerating(true);
            
            // Broadcast health update to all players
            broadcastHealthRegeneration(gameId, playerState.getSlot(), newHP - oldHP, 
                    playerState.getCurrentHP(), playerState.getMaxHP());
            
            log.debug("Player slot {} regenerated {} HP in game {} ({}->{})", 
                    playerState.getSlot(), newHP - oldHP, gameId, oldHP, newHP);
        } else {
            playerState.setRegenerating(false);
        }
    }
    
    /**
     * Broadcast health regeneration update to all players in the game
     */
    private void broadcastHealthRegeneration(String gameId, short slot, int healAmount, 
            int currentHealth, int maxHealth) {
        
        // Create health update message (negative damage = healing)
        HealthUpdateSend healthUpdate = new HealthUpdateSend(slot, currentHealth, maxHealth, 
                -healAmount, System.currentTimeMillis());
        
        // Broadcast to all players in the game
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null && !gameChannels.isEmpty()) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(healthUpdate);
                }
            });
            
            log.debug("Broadcasted health regeneration for slot {} in game {} - HP: {}/{} (+{})", 
                    slot, gameId, currentHealth, maxHealth, healAmount);
        }
    }
    
    /**
     * Process health regeneration for all active games
     */
    public void processAllGamesRegeneration(Set<String> activeGames) {
        for (String gameId : activeGames) {
            try {
                processHealthRegeneration(gameId);
            } catch (Exception e) {
                log.error("Error processing health regeneration for game: {}", gameId, e);
            }
        }
    }
}
