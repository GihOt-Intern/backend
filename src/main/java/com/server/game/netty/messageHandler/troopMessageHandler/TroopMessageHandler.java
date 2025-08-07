package com.server.game.netty.messageHandler.troopMessageHandler;

import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.SlotInfo;
import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.troop.TroopPositionReceive;
import com.server.game.netty.receiveObject.troop.TroopSpawnReceive;
import com.server.game.netty.sendObject.troop.TroopSpawnSend;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.troop.TroopManager;
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

    @MessageMapping(TroopPositionReceive.class)
    public void handleTroopPosition(TroopPositionReceive request, ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        String gameId = ChannelManager.getGameIdByChannel(channel);
        Short requestingSlot = ChannelManager.getSlotByChannel(channel);
        if (gameId == null || requestingSlot == null) {
            log.warn("Game ID or slot not found for channel when processing troop position");
            return;
        }

        GameState gameState = gameCoordinator.getGameState(gameId);
        if (gameState == null) {
            log.warn("No game state found for game ID: {}", gameId);
            return;
        }

        // Process each troop position and spread them out
        List<TroopPositionReceive.PositionData> positions = request.getPositions();
        if (positions == null || positions.isEmpty()) {
            log.warn("No troop positions provided in request");
            return;
        }

        // Spread out the positions to prevent collisions
        List<Vector2> spreadPositions = spreadTroopPositions(positions, gameState);
        
        // Apply the spread positions to each troop
        for (int i = 0; i < positions.size() && i < spreadPositions.size(); i++) {
            TroopPositionReceive.PositionData posData = positions.get(i);
            Vector2 newPosition = spreadPositions.get(i);
            
            // Verify the troop belongs to the requesting slot for security
            Entity troopEntity = gameState.getEntityByStringId(posData.getTroopId());
            if (troopEntity == null || !(troopEntity instanceof TroopInstance2)) {
                log.warn("Troop {} not found or invalid type", posData.getTroopId());
                continue;
            }
            
            TroopInstance2 troop = (TroopInstance2) troopEntity;
            if (troop.getOwnerSlot().getSlot() != requestingSlot) {
                log.warn("Player {} attempted to move troop {} owned by slot {}", 
                    requestingSlot, posData.getTroopId(), troop.getOwnerSlot().getSlot());
                continue;
            }
            
            // Set the new position for the troop
            troopManager.setMovePosition(gameId, posData.getTroopId(), newPosition);
            
            log.debug("Moved troop {} to spread position {}", posData.getTroopId(), newPosition);
        }
        
        log.info("Processed {} troop positions with collision avoidance for game {}", positions.size(), gameId);
    }

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
                troopManager.setAttackTarget(gameId, troopInstance.getStringId(), slotState.getChampion().getStringId());
            }
        } else {
            var minionPosition = getMinionPositionForSlot(gameState, request.getOwnerSlot());
            if (minionPosition != null) {
                log.info("Spawning minion troop of type {} for owner slot {}", troopType, request.getOwnerSlot());
                log.info("Minion position: {}", minionPosition);
                troopManager.setMovePosition(gameId, troopInstance.getStringId(), minionPosition);
            }
        }

        broadcastTroopSpawn(gameId, troopInstance.getStringId(), request.getTroopId(), request.getOwnerSlot(), spawnPosition, troopInstance.getMaxHP());

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

    private void broadcastTroopSpawn(String gameId, String troopId, short troopType, short ownerSlot, Vector2 position, int maxHP) {
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
            maxHP,
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

    /**
     * Spreads out troop positions to maintain at least 2 cells distance between troops
     * Each cell is assumed to be 1 unit, so 2 cells = 2.0 units minimum distance
     */
    private List<Vector2> spreadTroopPositions(List<TroopPositionReceive.PositionData> originalPositions, GameState gameState) {
        final float MIN_DISTANCE = 2.0f; // 2 cells minimum distance
        final float SPREAD_FACTOR = 1.5f; // How much to spread when resolving conflicts
        final int MAX_ITERATIONS = 10; // Prevent infinite loops
        
        List<Vector2> spreadPositions = new ArrayList<>();
        
        // Convert original positions to Vector2
        for (TroopPositionReceive.PositionData posData : originalPositions) {
            spreadPositions.add(new Vector2(posData.getX(), posData.getY()));
        }
        
        // Iteratively resolve conflicts
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            boolean hasConflicts = false;
            
            // Check all pairs for conflicts
            for (int i = 0; i < spreadPositions.size(); i++) {
                for (int j = i + 1; j < spreadPositions.size(); j++) {
                    Vector2 pos1 = spreadPositions.get(i);
                    Vector2 pos2 = spreadPositions.get(j);
                    
                    float distance = pos1.distance(pos2);
                    if (distance < MIN_DISTANCE && distance > 0) {
                        hasConflicts = true;
                        
                        // Calculate direction to separate the positions
                        Vector2 direction = pos2.subtract(pos1).normalize();
                        
                        // If positions are identical, use a random direction
                        if (direction.length() == 0) {
                            direction = new Vector2(1.0f, 0.0f);
                        }
                        
                        // Calculate how much to move each position
                        float requiredSeparation = MIN_DISTANCE - distance;
                        float moveDistance = (requiredSeparation / 2.0f) * SPREAD_FACTOR;
                        
                        // Move positions apart
                        Vector2 offset = direction.multiply(moveDistance);
                        spreadPositions.set(i, pos1.subtract(offset));
                        spreadPositions.set(j, pos2.add(offset));
                        
                        log.trace("Resolved collision between positions {} and {}, moved apart by {}", 
                            pos1, pos2, moveDistance * 2);
                    }
                }
            }
            
            // If no conflicts found, we're done
            if (!hasConflicts) {
                log.debug("Position spreading completed after {} iterations", iteration + 1);
                break;
            }
            
            if (iteration == MAX_ITERATIONS - 1) {
                log.warn("Position spreading reached maximum iterations, some conflicts may remain");
            }
        }
        
        // Validate all positions are still valid (optional: check map bounds)
        for (int i = 0; i < spreadPositions.size(); i++) {
            Vector2 pos = spreadPositions.get(i);
            if (pos.x() < 0 || pos.y() < 0) {
                // Clamp to positive coordinates if needed
                spreadPositions.set(i, new Vector2(Math.max(0, pos.x()), Math.max(0, pos.y())));
                log.debug("Clamped position {} to positive coordinates", pos);
            }
        }
        
        log.debug("Spread {} troop positions with minimum distance of {} units", 
            spreadPositions.size(), MIN_DISTANCE);
        
        return spreadPositions;
    }
}
