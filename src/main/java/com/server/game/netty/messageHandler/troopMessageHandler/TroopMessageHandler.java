package com.server.game.netty.messageHandler.troopMessageHandler;

import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.SlotInfo;
import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.troop.TroopSpawnReceive;
import com.server.game.netty.sendObject.troop.TroopSpawnSend;
import com.server.game.service.troop.TroopManager;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.move.MoveService;
import com.server.game.util.TroopEnum;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TroopMessageHandler {
    TroopManager troopManager;
    GameCoordinator gameCoordinator;
    MoveService moveService;

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

        boolean isAttack = request.isAttack();
        if (isAttack) {
            log.info("Spawning attack troop of type {} for owner slot {}", troopType, request.getOwnerSlot());
            SlotState slotState = gameState.getSlotState(request.getOwnerSlot());
            if (slotState != null && slotState.getChampion() != null) {
                troopInstance.setMove2Target(slotState.getChampion());
            }
        } else {
            var minionPosition = getMinionPositionForSlot(gameState, request.getOwnerSlot());
            if (minionPosition != null) {
                log.info("Spawning minion troop of type {} for owner slot {}", troopType, request.getOwnerSlot());
                log.info("Minion position: {}", minionPosition);
                moveService.setMove(troopInstance, minionPosition, true);
            }
        }

        broadcastTroopSpawn(gameId, troopInstance.getStringId(), request.getTroopId(), request.getOwnerSlot(), spawnPosition);

        log.info("Troop spawned successfully: {} at position {}", troopType, spawnPosition);
    }

    private Vector2 getMinionPositionForSlot(GameState gameState, short ownerSlot) {
        try {
            GameMap gameMap = gameState.getGameMap();
            if (gameMap == null) {
                log.warn("GameMap not found in GameState");
                return null;
            }

            SlotInfo slotInfo = gameMap.getSlot2SlotInfo().get(ownerSlot);
            if (slotInfo == null) {
                log.warn("SlotInfo not found for slot: {}", ownerSlot);
                return null;
            }

            List<Vector2> minionPositions = slotInfo.getMinionPositions();
            if (minionPositions == null || minionPositions.size() < 4) {
                log.warn("minion_positions requires at least 4 points to define the rectangle.");
                return null;
            }

            // Assuming the points define a rectangle, find the min/max X and Y
            float minX = Float.MAX_VALUE, maxX = -1000.f;
            float minY = Float.MAX_VALUE, maxY = -1000.f;

            // Calculate the min and max for both X and Y coordinates
            for (Vector2 pos : minionPositions) {
                log.info("Minion position: {}", pos);
                minX = Math.min(minX, pos.x());
                maxX = Math.max(maxX, pos.x());
                minY = Math.min(minY, pos.y());
                maxY = Math.max(maxY, pos.y());
            }

            log.info("Minion position bounds: minX={}, maxX={}, minY={}, maxY={}", minX, maxX, minY, maxY);
            float randomX = minX + (float) (Math.random() * (maxX - minX));
            float randomY = minY + (float) (Math.random() * (maxY - minY));
            log.info("Random minion position: ({}, {})", randomX, randomY);

            return new Vector2(randomX, randomY);
        } catch (Exception e) {
            log.error("Error getting minion positions for slot {}: {}", ownerSlot, e.getMessage(), e);
        }
        
        return null;
    }

    private Vector2 determineSpawnPosition(GameState gameState, short ownerSlot) {
        // Logic to determine spawn position based on game state and owner slot
        // For simplicity, we can use a fixed spawn position or a position defined in the game state
        return gameState.getSpawnPosition(gameState.getSlotState(ownerSlot));
    }

    private void broadcastTroopSpawn(String gameId, String troopId, short troopType, short ownerSlot, Vector2 position) {
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels == null || gameChannels.isEmpty()) {
            log.warn("No active channels found for game ID: {}", gameId);
            return;
        }

        // Calculate rotation based on position to the center point (0,0)
        float rotate = (float) Math.atan2(position.y(), position.x());

        // Create the TroopSpawnSend object
        TroopSpawnSend troopSpawnSend = new TroopSpawnSend(
            troopId,
            troopType,
            ownerSlot,
            position.x(),
            position.y(),
            rotate,
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
