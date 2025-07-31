package com.server.game.netty.handler.troopHandler;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.model.game.GameState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.SlotInfo;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.troop.TroopSpawnReceive;
import com.server.game.netty.sendObject.troop.TroopSpawnSend;
import com.server.game.service.troop.TroopManager;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.util.TroopEnum;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TroopHandler {
    TroopManager troopManager;
    GameCoordinator gameCoordinator;

    /**
     * xử lý việc người chơi spawn quân đội
     */
    @MessageMapping(TroopSpawnReceive.class)
    public void handleTroopSpawn(TroopSpawnReceive request, ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        String gameId = ChannelManager.getGameIdByChannel(channel);
        Short requestingSlot = ChannelManager.getSlotByChannel(channel);

        if (gameId == null || requestingSlot == null) {
            log.warn("Game ID or slot not found for channel when processing troop spawn");
            return;
        }

        if (requestingSlot != request.getOwnerSlot()) {
            log.warn("Slot mismatch in troop spawn request: expected {}, received {}", requestingSlot, request.getOwnerSlot());
            return;
        }

        // Convert troopId to TroopEnum
        TroopEnum troopType;
        try {
            troopType = TroopEnum.fromShort(request.getTroopId());
        } catch (IllegalArgumentException e) {
            log.error("Invalid troop ID: {}", request.getTroopId(), e);
            return;
        }

        GameState gameState = gameCoordinator.getGameState(gameId);
        if (gameState == null) {
            log.warn("No game state found for game ID: {}", gameId);
            return;
        }

        Vector2 spawnPosition = determineSpawnPosition(gameState, request.getOwnerSlot());
        if (spawnPosition == null) {
            log.warn("Could not determine spawn position for troop type: {}", troopType);
            return;
        }

        TroopInstance2 troopInstance = troopManager.createTroop(
            gameId,
            request.getOwnerSlot(),
            troopType,
            spawnPosition
        );
        if (troopInstance == null) {
            log.warn("Failed to create troop instance for type: {}", troopType);
            return;
        }
        
        // Set a random position within the rectangle formed by minion positions as the target
        Vector2 targetPosition = determineTargetPosition(gameState, request.getOwnerSlot());
        if (targetPosition != null) {
            troopInstance.setMoveTarget(targetPosition);
            troopInstance.setAIState(TroopInstance2.TroopAIState.MOVING_TO_POSITION);
            log.debug("Set target position for troop {}: {}", troopInstance.getStringId(), targetPosition);
        }

        broadcastTroopSpawn(gameId, troopInstance.getStringId(), request.getTroopId(), request.getOwnerSlot(), spawnPosition);

        log.info("Troop spawned successfully: {} at position {} with target position {}", 
            troopType, spawnPosition, targetPosition);
    }

    private Vector2 determineSpawnPosition(GameState gameState, short ownerSlot) {
        // Always use the champion spawn position for initial troop spawn
        Vector2 spawnPosition = gameState.getSpawnPosition(ownerSlot);
        
        if (spawnPosition == null) {
            log.warn("No spawn position found for slot {}", ownerSlot);
            return null;
        }
        
        return spawnPosition;
    }
    
    /**
     * Calculate a random position within the rectangle formed by the minion positions
     */
    private Vector2 determineTargetPosition(GameState gameState, short ownerSlot) {
        List<SlotInfo> slotInfos = gameState.getSlotInfos();
        
        for (SlotInfo slotInfo : slotInfos) {
            if (slotInfo.getSlot() == ownerSlot) {
                List<Vector2> minionPositions = slotInfo.getMinionPositions();
                
                if (minionPositions != null && minionPositions.size() >= 4) {
                    // Find min and max x, y values to determine the rectangle bounds
                    float minX = Float.MAX_VALUE;
                    float minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE;
                    float maxY = Float.MIN_VALUE;
                    
                    for (Vector2 pos : minionPositions) {
                        minX = Math.min(minX, pos.x());
                        minY = Math.min(minY, pos.y());
                        maxX = Math.max(maxX, pos.x());
                        maxY = Math.max(maxY, pos.y());
                    }
                    
                    // Calculate a random position within the rectangle
                    float randomX = minX + (float) Math.random() * (maxX - minX);
                    float randomY = minY + (float) Math.random() * (maxY - minY);
                    
                    log.debug("Generated random target position ({}, {}) for slot {} within bounds: [{}, {}, {}, {}]", 
                             randomX, randomY, ownerSlot, minX, minY, maxX, maxY);
                    
                    return new Vector2(randomX, randomY);
                } else {
                    // Fall back to a random minion position if available
                    return gameState.getRandomMinionPosition(ownerSlot);
                }
            }
        }
        
        log.warn("Could not find slot info for slot {}", ownerSlot);
        return null;
    }

    private void broadcastTroopSpawn(String gameId, String troopId, short troopType, short ownerSlot, Vector2 position) {
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels == null || gameChannels.isEmpty()) {
            log.warn("No active channels found for game ID: {}", gameId);
            return;
        }

        log.info("Troop ID: {}, Game Channels: {}", troopId, gameChannels);
        TroopSpawnSend troopSpawnSend = new TroopSpawnSend(
            troopId,
            troopType,
            ownerSlot,
            position.x(),
            position.y(),
            System.currentTimeMillis()
        );

        // Broadcast the troop spawn message to all players in the game
        for (Channel playerChannel : gameChannels) {
            if (playerChannel.isActive()) {
                playerChannel.writeAndFlush(troopSpawnSend);
            } else {
                log.warn("Inactive channel found for game ID: {}, slot: {}", gameId, ownerSlot);
            }
        } 
    }
}
