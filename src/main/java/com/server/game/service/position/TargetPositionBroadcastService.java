package com.server.game.service.position;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.TargetPositionSend;
import com.server.game.netty.sendObject.TargetPositionSend.TargetPositionData;
import com.server.game.service.troop.TroopManager;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetPositionBroadcastService {
    
    final TroopManager troopManager;
    
    // gameId -> Map<troopInstanceId, Vector2>
    final Map<String, Map<String, Vector2>> previousTroopPositions = new ConcurrentHashMap<>();
    
    /**
     * Unregister a game - called from GameScheduler
     */
    public void unregisterGame(String gameId) {
        previousTroopPositions.remove(gameId);
        log.info("Unregistered game from target position broadcasting: {}", gameId);
    }
    
    /**
     * Broadcast troop positions for a specific game - called from GameScheduler
     */
    public void broadcastTroopPositions(String gameId) {
        // Get all troops for this game
        Collection<TroopInstance2> troops = troopManager.getGameTroops(gameId);
        
        if (troops.isEmpty()) {
            return; // No troops to broadcast
        }
        
        // Get previous positions for comparison
        Map<String, Vector2> oldPositions = previousTroopPositions.computeIfAbsent(gameId, k -> new HashMap<>());
        
        // Create list for troops with changed positions
        List<TargetPositionData> targetDataList = new ArrayList<>();
        
        for (TroopInstance2 troop : troops) {
            if (!troop.isAlive()) {
                continue; // Skip dead troops
            }
            
            String troopId = troop.getIdAString();
            Vector2 currentPosition = troop.getCurrentPosition();
            Vector2 oldPosition = oldPositions.get(troopId);
            
            // Check if position has changed
            if (hasPositionChanged(oldPosition, currentPosition)) {
                targetDataList.add(new TargetPositionData(troopId, currentPosition));
                
                // Update old position cache
                oldPositions.put(troopId, currentPosition);
            }
        }
        
        // Broadcast if any troops have changed position
        if (!targetDataList.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            TargetPositionSend targetPositionSend = new TargetPositionSend(targetDataList, currentTime);
            log.debug("Broadcasting troop positions for game {}: {}", gameId, targetPositionSend);
            
            Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
            if (channel != null) {
                channel.writeAndFlush(targetPositionSend); // sendTarget of message is AMatchBroadcastTarget
            } else {
                // No players in game, no need to broadcast, remove game from activeGames
                unregisterGame(gameId);
            }
        }
    }
    
    /**
     * Remove a troop from position tracking when it's removed from the game
     */
    public void removeTroopPosition(String gameId, String troopId) {
        Map<String, Vector2> positions = previousTroopPositions.get(gameId);
        if (positions != null) {
            positions.remove(troopId);
        }
    }
    
    /**
     * Check if position has changed since last broadcast
     */
    private boolean hasPositionChanged(Vector2 oldPosition, Vector2 newPosition) {
        if (oldPosition == null) {
            return true; // First time, consider changed
        }
        
        // Compare positions with a small change threshold
        float threshold = 0.01f; // Minimum change threshold
        
        Vector2 delta = newPosition.subtract(oldPosition);
        float distance = delta.length();
        return distance > threshold;
    }
}
