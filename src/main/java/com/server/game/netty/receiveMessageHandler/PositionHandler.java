package com.server.game.netty.receiveMessageHandler;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.receiveObject.PositionReceive;
import com.server.game.service.AttackTargetingService;
import com.server.game.service.MoveService;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PositionHandler {
    private final MoveService moveService;
    private final AttackTargetingService attackTargetingService;
    
    // Rate limiting: minimum time between position updates (in milliseconds)
    private static final long MIN_UPDATE_INTERVAL = 50; // 50ms = max 20 updates per second
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    
    @MessageMapping(PositionReceive.class)
    public void handlePosition(PositionReceive receiveObject, Channel channel) {

        String gameId = ChannelManager.getGameIdByChannel(channel);
        Short slot = receiveObject.getSlot();
        long timestamp = receiveObject.getTimestamp();
        
        if (gameId == null || slot == null) {
            System.out.println(">>> Invalid gameId for position update");
            return;
        }
        
        // Kiểm tra slot có hợp lệ không (chống hack)
        short expectedSlot = ChannelManager.getSlotByChannel(channel);
        if (slot != expectedSlot) {
            System.out.println(">>> Slot mismatch: received " + slot + ", expected " + expectedSlot);
            return;
        }

        // Rate limiting check
        String playerKey = gameId + ":" + slot;
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(playerKey);
        
        if (lastUpdate != null && (currentTime - lastUpdate) < MIN_UPDATE_INTERVAL) {
            log.debug("Rate limit exceeded for player {}:{} - ignoring update", gameId, slot);
            return;
        }
        
        // Update the last update time
        lastUpdateTime.put(playerKey, currentTime);

        // Clear attack target when player manually moves
        attackTargetingService.clearAttackTarget(gameId, slot);

        moveService.setMoveTarget(
            gameId,
            slot,
            receiveObject.getPosition()
        );

        System.out.println(">>> Position updated for gameId: " + gameId + ", slot: " + slot +
            ", X: " + receiveObject.getPosition().x() + ", Y: " + receiveObject.getPosition().y() + ", timestamp: " + timestamp);
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