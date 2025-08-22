package com.server.game.netty.messageHandler;

import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.SlotInfo;
import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.Troop;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.troop.TroopPositionReceive;
import com.server.game.netty.receiveObject.troop.TroopSpawnReceive;
import com.server.game.netty.sendObject.troop.TroopSpawnSend;
import com.server.game.netty.sendObject.troop.TroopCooldownSend;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TroopMessageHandler {
    TroopManager troopManager;
    GameCoordinator gameCoordinator;
    
    // Cooldown tracking: gameId:slot:troopType -> timestamp when cooldown ends
    private final Map<String, Long> troopCooldowns = new ConcurrentHashMap<>();
    private static final long TROOP_SPAWN_COOLDOWN_MS = 3000; // 3 seconds

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
        List<String> troopIds = request.getTroopIds();
        if (troopIds == null || troopIds.isEmpty()) {
            log.warn("No troop IDs provided in request");
            return;
        }

        // Get the original position from the request
        Vector2 originalPosition = new Vector2(request.getX(), request.getY());
        
        // Generate spread positions for all troops around the original position
        List<Vector2> spreadPositions = spreadTroopPositions(troopIds, originalPosition, gameState);
        
        // Apply the spread positions to each troop
        for (int i = 0; i < troopIds.size() && i < spreadPositions.size(); i++) {
            String troopId = troopIds.get(i);
            Vector2 newPosition = spreadPositions.get(i);
            
            // Verify the troop belongs to the requesting slot for security
            Entity troopEntity = gameState.getEntityByStringId(troopId);
            if (troopEntity == null || !(troopEntity instanceof Troop)) {
                log.warn("Troop {} not found or invalid type", troopId);
                continue;
            }
            
            Troop troop = (Troop) troopEntity;
            if (troop.getOwnerSlot().getSlot() != requestingSlot) {
                log.warn("Player {} attempted to move troop {} owned by slot {}", 
                    requestingSlot, troopId, troop.getOwnerSlot().getSlot());
                continue;
            }
            
            // Set the new position for the troop
            troopManager.setMovePosition(gameId, troopId, newPosition);
            
            log.debug("Moved troop {} to spread position {}", troopId, newPosition);
        }
        
        log.info("Processed {} troop positions with collision avoidance for game {}", troopIds.size(), gameId);
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

        // Check cooldown before spawning
        String cooldownKey = gameId + ":" + requestingSlot + ":" + troopType;
        long currentTime = System.currentTimeMillis();
        Long cooldownEndTime = troopCooldowns.get(cooldownKey);
        
        if (cooldownEndTime != null && currentTime < cooldownEndTime) {
            long remainingCooldown = (cooldownEndTime - currentTime) / 1000; // Convert to seconds
            log.info("Troop spawn rejected due to cooldown. Slot: {}, TroopType: {}, Remaining: {}s",
                requestingSlot, troopType, remainingCooldown);
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

        Troop troopInstance = troopManager.createTroop(
            gameId,
            request.getOwnerSlot(),
            troopType,
            spawnPosition
        );
        if (troopInstance == null) {
            log.warn("Failed to create troop instance for type: {}", troopType);
            return;
        }

        // Set cooldown after successful spawn
        troopCooldowns.put(cooldownKey, currentTime + TROOP_SPAWN_COOLDOWN_MS);
        
        // Send cooldown message to client
        TroopCooldownSend cooldownMessage = new TroopCooldownSend(request.getTroopId(), (short) (TROOP_SPAWN_COOLDOWN_MS / 1000));
        channel.writeAndFlush(cooldownMessage);

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

        log.info("Troop spawned successfully: {} at position {}, cooldown set for {} seconds", 
            troopType, spawnPosition, TROOP_SPAWN_COOLDOWN_MS / 1000);
    }

    /**
     * Clean up expired cooldowns for a specific game to prevent memory leaks
     */
    public void cleanupGameCooldowns(String gameId) {
        long currentTime = System.currentTimeMillis();
        troopCooldowns.entrySet().removeIf(entry -> 
            entry.getKey().startsWith(gameId + ":") && entry.getValue() <= currentTime
        );
        log.debug("Cleaned up expired cooldowns for game: {}", gameId);
    }

    /**
     * Get remaining cooldown time for a specific troop type and slot
     * @return remaining cooldown in milliseconds, or 0 if no cooldown
     */
    public long getRemainingCooldown(String gameId, short slot, TroopEnum troopType) {
        String cooldownKey = gameId + ":" + slot + ":" + troopType;
        Long cooldownEndTime = troopCooldowns.get(cooldownKey);
        
        if (cooldownEndTime == null) {
            return 0;
        }
        
        long remaining = cooldownEndTime - System.currentTimeMillis();
        return Math.max(0, remaining);
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
     * Arranges troop positions in a spiral pattern around the center position
     * The first position remains unchanged, subsequent positions spiral outward
     * with a minimum distance of 2 cells between each position
     */
    private List<Vector2> spreadTroopPositions(List<String> troopIds, Vector2 centerPosition, GameState gameState) {
        final float CELL_DISTANCE = 2.0f; // 2 cells distance between positions
        
        List<Vector2> spreadPositions = new ArrayList<>();
        
        if (troopIds.isEmpty()) {
            return spreadPositions;
        }
        
        // First position remains the same (center position)
        spreadPositions.add(centerPosition);
        
        log.debug("Center position (first troop): {}", centerPosition);
        
        // If only one troop, return early
        if (troopIds.size() == 1) {
            return spreadPositions;
        }
        
        // Generate spiral positions for the remaining troops
        // Spiral directions: Left -> Up -> Right -> Down (counter-clockwise)
        Vector2[] directions = {
            new Vector2(-CELL_DISTANCE, 0),  // Left
            new Vector2(0, CELL_DISTANCE),   // Up  
            new Vector2(CELL_DISTANCE, 0),   // Right
            new Vector2(0, -CELL_DISTANCE)   // Down
        };
        
        Vector2 currentPosition = centerPosition;
        int directionIndex = 0; // Start with Left direction
        int stepsInCurrentDirection = 1; // How many steps to take in current direction
        int stepsTaken = 0; // Steps taken in current direction
        int stepsBeforeDirectionChange = 1; // After how many steps to change direction
        
        // Place remaining troops in spiral pattern
        for (int i = 1; i < troopIds.size(); i++) {
            // Move to next position in current direction
            Vector2 direction = directions[directionIndex];
            currentPosition = currentPosition.add(direction);
            spreadPositions.add(currentPosition);
            
            log.trace("Troop {} positioned at {} (direction: {})", 
                i, currentPosition, getDirectionName(directionIndex));
            
            stepsTaken++;
            
            // Check if we need to change direction
            if (stepsTaken >= stepsInCurrentDirection) {
                directionIndex = (directionIndex + 1) % 4; // Move to next direction
                stepsTaken = 0;
                
                // After completing Left and Right directions, increase steps
                if (directionIndex == 2 || directionIndex == 0) { // Right or Left
                    stepsBeforeDirectionChange++;
                    stepsInCurrentDirection = stepsBeforeDirectionChange;
                } else { // Up or Down
                    stepsInCurrentDirection = stepsBeforeDirectionChange;
                }
            }
        }
        
        log.debug("Generated {} spread positions in spiral pattern", spreadPositions.size());
        return spreadPositions;
    }
    
    /**
     * Helper method to get direction name for logging
     */
    private String getDirectionName(int directionIndex) {
        switch (directionIndex) {
            case 0: return "LEFT";
            case 1: return "UP";
            case 2: return "RIGHT";
            case 3: return "DOWN";
            default: return "UNKNOWN";
        }
    }
}
