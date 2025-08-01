package com.server.game.netty.sender;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.model.game.Entity;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.PositionReceive;
// import com.server.game.service.attack.AttackTargetingService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.move.MoveService;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class PositionSender {
    private final MoveService moveService;

    // @Lazy
    // private AttackTargetingService attackTargetingService;
    
    private GameStateService gameStateService;
    
    
    // Rate limiting: minimum time between position updates (in milliseconds)
    private static final long MIN_UPDATE_INTERVAL = 50; // 50ms = max 20 updates per second
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    
    @MessageMapping(PositionReceive.class)
    public void handlePosition(PositionReceive receiveObject, Channel channel) {

        String gameId = ChannelManager.getGameIdByChannel(channel);
        
        String entityStringId = receiveObject.getStringId();

        Entity entity = gameStateService.getEntityByStringId(gameId, entityStringId);
        if (entity == null) {
            log.debug("Entity not found for stringId: {}", entityStringId);
            return;
        }

        // Short slot = ChannelManager.getSlotByChannel(channel);

        long clientTimestamp = receiveObject.getTimestamp();
        
        if (gameId == null) {
            log.debug("Invalid gameId for position update");
            return;
        }
        
        // Rate limiting check
        String playerKey = gameId + ":" + entityStringId;
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(playerKey);
        
        if (lastUpdate != null && (currentTime - lastUpdate) < MIN_UPDATE_INTERVAL) {
            log.debug("Rate limit exceeded for entity {}:{} - ignoring update", gameId, entityStringId);
            return;
        }
        
        // Update the last update time
        lastUpdateTime.put(playerKey, currentTime);

        // Clear attack target when player manually moves
        // attackTargetingService.clearAttackTarget(gameId, slot);

        moveService.setMove(entity, receiveObject.getPosition());

        System.out.println(">>> [Log in PositionHandler.handlePosition] Received position update for entity: " + entityStringId + ", position: " + receiveObject.getPosition() + ", timestamp: " + clientTimestamp);
    }
    
    /**
     * Clean up old entries from rate limiter cache to prevent memory leaks
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000)
    public void cleanupRateLimiterCache() {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = currentTime - (MIN_UPDATE_INTERVAL * 1000); // Remove entries older than 50 seconds
        
        Iterator<Map.Entry<String, Long>> iterator = lastUpdateTime.entrySet().iterator();
        int removed = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < cleanupThreshold) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            log.debug("Cleaned up {} old rate limiter entries", removed);
        }
    }
} 