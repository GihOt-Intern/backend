package com.server.game.netty.handler.troopHandler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.map.component.Vector2;
import com.server.game.model.gameState.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.troop.TroopSpawnReceive;
import com.server.game.netty.sendObject.troop.TroopSpawnSend;
import com.server.game.service.troop.TroopManager;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.troop.TroopInstance;
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

        TroopInstance troopInstance = troopManager.createTroop(
            gameId,
            request.getOwnerSlot(),
            troopType,
            spawnPosition
        );
        if (troopInstance == null) {
            log.warn("Failed to create troop instance for type: {}", troopType);
            return;
        }

        broadcastTroopSpawn(gameId, request.getTroopId(), request.getOwnerSlot(), spawnPosition);

        log.info("Troop spawned successfully: {} at position {}", troopType, spawnPosition);
    }

    private Vector2 determineSpawnPosition(GameState gameState, short ownerSlot) {
        // Logic to determine spawn position based on game state and owner slot
        // For simplicity, we can use a fixed spawn position or a position defined in the game state
        return gameState.getSpawnPosition(ownerSlot);
    }

    private void broadcastTroopSpawn(String gameId, short troopType, short ownerSlot, Vector2 position) {
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels == null || gameChannels.isEmpty()) {
            log.warn("No active channels found for game ID: {}", gameId);
            return;
        }

        TroopSpawnSend troopSpawnSend = new TroopSpawnSend(
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
