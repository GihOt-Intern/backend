package com.server.game.service.move;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.server.game.model.gameState.GameState;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.move.MoveService.MoveTarget.PathComponent;
import com.server.game.service.position.PositionService;
import com.server.game.util.ThetaStarPathfinder;

import lombok.Data;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MoveService {
    
    @Autowired
    private PositionService positionService;

    @Autowired
    @Lazy
    private GameCoordinator gameCoordinator;


    private final Map<String, Map<Short, MoveTarget>> moveTargets = new ConcurrentHashMap<>();

    // Minimum distance threshold to avoid unnecessary pathfinding
    private static final float MIN_MOVE_DISTANCE = 0.5f;
    
    // Service-level rate limiting - Reduced to allow more responsive movement
    private static final long MIN_MOVE_TARGET_INTERVAL = 50; // 50ms = max 20 updates per second
    private final Map<String, Long> lastMoveTargetTime = new ConcurrentHashMap<>();
    
    // Track failed pathfinding attempts to prevent repeated invalid requests
    private final Map<String, Integer> failedPathfindingCount = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5; // Increased threshold
    private static final long FAILED_ATTEMPT_COOLDOWN = 300; // Reduced cooldown

    /**
     * Đặt mục tiêu di chuyển mới cho người chơi
     */
    public void setMoveTarget(String gameId, short slot, Vector2 targetPosition) {
        
        // Service-level rate limiting
        String playerKey = gameId + ":" + slot;
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMoveTargetTime.get(playerKey);
        
        if (lastTime != null && (currentTime - lastTime) < MIN_MOVE_TARGET_INTERVAL) {
            log.debug("Service-level rate limit exceeded for player {}:{} - ignoring move request", gameId, slot);
            return;
        }
        
        // Check for repeated failed pathfinding attempts
        Integer failCount = failedPathfindingCount.get(playerKey);
        if (failCount != null && failCount >= MAX_FAILED_ATTEMPTS) {
            Long lastFailTime = lastMoveTargetTime.get(playerKey + ":fail");
            if (lastFailTime != null && (currentTime - lastFailTime) < FAILED_ATTEMPT_COOLDOWN) {
                log.debug("Player {}:{} in cooldown due to repeated failed pathfinding attempts", gameId, slot);
                return;
            } else {
                // Reset failure count after cooldown
                failedPathfindingCount.remove(playerKey);
            }
        }
        
        lastMoveTargetTime.put(playerKey, currentTime);
        
        GameState gameState = gameCoordinator.getGameState(gameId);
        float slotSpeed = gameState.getSpeed(slot);
        
        // Get the most up-to-date position using the existing method
        Vector2 startPosition = getCurrentRealTimePosition(gameId, slot);
        
        // If no position found, fallback to default behavior
        if (startPosition == null) {
            PositionData currentPos = positionService.getPlayerPosition(gameId, slot);
            
            if (currentPos == null) {
                currentPos = new PositionData(
                    targetPosition,
                    slotSpeed,
                    System.currentTimeMillis()
                );

                positionService.updatePendingPosition(
                    gameId, 
                    slot, 
                    targetPosition,
                    slotSpeed,
                    System.currentTimeMillis()
                );
                return;
            }
            
            startPosition = currentPos.getPosition();
        }

        // Check if the move distance is too small to avoid unnecessary calculations
        float moveDistance = startPosition.distance(targetPosition);
        if (moveDistance < MIN_MOVE_DISTANCE) {
            log.debug("Move distance {} is too small for slot {}, ignoring", moveDistance, slot);
            return;
        }

        GameMapGrid gameMapGrid = gameState.getGameMapGrid();

        GridCell startCell = gameState.toGridCell(startPosition);
        GridCell targetCell = gameState.toGridCell(targetPosition);

        log.info("Setting move target for slot {}: from {} to {}", slot, startPosition, targetPosition);
        log.info("Calculating path for slot {} from cell {} to cell {}", slot, startCell, targetCell);

        List<GridCell> path = ThetaStarPathfinder.findPath(gameMapGrid.getGrid(), startCell, targetCell);
        
        // Check if pathfinding failed or returned empty path
        if (path == null || path.isEmpty()) {
            log.warn("Pathfinding failed for player {}:{} from {} to {}", gameId, slot, startPosition, targetPosition);
            
            // Track failed attempts
            int currentFailCount = failedPathfindingCount.getOrDefault(playerKey, 0) + 1;
            failedPathfindingCount.put(playerKey, currentFailCount);
            lastMoveTargetTime.put(playerKey + ":fail", currentTime);
            
            if (currentFailCount >= MAX_FAILED_ATTEMPTS) {
                log.warn("Player {}:{} has {} failed pathfinding attempts, applying {}ms cooldown", 
                        gameId, slot, currentFailCount, FAILED_ATTEMPT_COOLDOWN);
            }
            
            // Return early to prevent further processing
            return;
        }
        
        // Reset failure count on successful pathfinding
        failedPathfindingCount.remove(playerKey);
        
        PathComponent pathComponent = new PathComponent(path);

        MoveTarget target = new MoveTarget(
            startPosition,
            targetPosition,
            System.currentTimeMillis(),
            slotSpeed,
            pathComponent
        );

        // Lưu mục tiêu và xoá mục tiêu cũ nếu có
        moveTargets.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
            .put(slot, target);
    }

    /**
     * Cập nhập vị trí dựa trên thời gian và mục tiêu di chuyển
     * Được gọi mỗi lần trước khi broadcast vị trí
     */
    public void updatePositions(String gameId) {
        Map<Short, MoveTarget> targets = moveTargets.get(gameId);
        if (targets == null || targets.isEmpty()) {
            return;
        }

        GameState gameState = gameCoordinator.getGameState(gameId);
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<Short, MoveTarget> entry : targets.entrySet()) {
            short slot = entry.getKey();
            MoveTarget target = entry.getValue();

            float elapsedTime = (currentTime - target.getStartTime()) / 1000.0f;
            float targetSpeed = target.getSpeed();

            Vector2 position = target.getCurrentPosition();
            float totalDistanceCovered = targetSpeed * elapsedTime;
            float remainingDistance = totalDistanceCovered;

            boolean reachedFinalDestination = false;
            while (target.path.hasNext() && !reachedFinalDestination) {
                GridCell nextCell = target.peekNextCell();
                Vector2 nextPosition = gameState.toPosition(nextCell);

                float distanceToNext = position.distance(nextPosition);

                if (remainingDistance >= distanceToNext) {
                    position = nextPosition;
                    remainingDistance -= distanceToNext;

                    target.path.getNextCell(); // Di chuyển đến ô tiếp theo

                    if (!target.path.hasNext()) {
                        reachedFinalDestination = true;
                    }
                } else {
                    Vector2 direction = nextPosition.subtract(position).normalize();
                    position = position.add(direction.multiply(remainingDistance));
                    
                    break;
                }
            }

            positionService.updatePendingPosition(
                gameId,
                slot,
                position,
                targetSpeed,
                currentTime
            );

            if (reachedFinalDestination || !target.path.hasNext()) {
                targets.remove(slot);
            }
        }
    }

    public void clearMoveTargets(String gameId) {
        moveTargets.remove(gameId);
    }
    
    /**
     * Clear move target for a specific player
     */
    public void clearMoveTarget(String gameId, short slot) {
        Map<Short, MoveTarget> targets = moveTargets.get(gameId);
        if (targets != null) {
            targets.remove(slot);
        }
    }



    /**
     * Get the current real-time position of a player (whether moving or not)
     */
    public Vector2 getCurrentRealTimePosition(String gameId, short slot) {
        // First check if player is currently moving
        Map<Short, MoveTarget> targets = moveTargets.get(gameId);
        if (targets != null && targets.containsKey(slot)) {
            // Player is moving - calculate current real-time position
            MoveTarget currentTarget = targets.get(slot);
            GameState gameState = gameCoordinator.getGameState(gameId);
            
            if (gameState == null) {
                return null;
            }
            
            // Calculate current position based on elapsed time
            long currentTime = System.currentTimeMillis();
            float elapsedTime = (currentTime - currentTarget.getStartTime()) / 1000.0f;
            float speed = currentTarget.getSpeed();
            
            Vector2 position = currentTarget.getCurrentPosition();
            float totalDistanceCovered = speed * elapsedTime;
            float remainingDistance = totalDistanceCovered;
            
            // Create a copy of the path to simulate without modifying the original
            List<GridCell> pathCopy = new ArrayList<>(currentTarget.path.path);
            int currentIndex = currentTarget.path.index;
            
            // Simulate the same movement logic as updatePositions
            while (currentIndex < pathCopy.size() && remainingDistance > 0.01f) {
                GridCell nextCell = pathCopy.get(currentIndex);
                Vector2 nextPosition = gameState.toPosition(nextCell);
                
                float distanceToNext = position.distance(nextPosition);
                
                if (remainingDistance >= distanceToNext) {
                    position = nextPosition;
                    remainingDistance -= distanceToNext;
                    currentIndex++; // Advance simulation index
                } else {
                    Vector2 direction = nextPosition.subtract(position).normalize();
                    position = position.add(direction.multiply(remainingDistance));
                    break;
                }
            }
            
            return position;
        } else {
            // Player is not moving - try to get the most recent position
            // First check pending position (most up-to-date)
            PositionData pendingPos = positionService.getPendingPlayerPosition(gameId, slot);
            if (pendingPos != null) {
                return pendingPos.getPosition();
            }
            
            // Fall back to cached position
            PositionData cachedPos = positionService.getPlayerPosition(gameId, slot);
            return cachedPos != null ? cachedPos.getPosition() : null;
        }
    }






    @Data
    public static class MoveTarget {
        private final Vector2 currentPosition;
        private final Vector2 targetPosition;
        private final long startTime;
        private final float speed;
        @Delegate
        private final PathComponent path;




        public static class PathComponent {
            private List<GridCell> path;
            private int index;

            public PathComponent(List<GridCell> path) {
                this.path = path;
                this.index = 0;
            }

            public boolean hasNext() {
                return index < path.size();
            }

            public GridCell peekNextCell() {
                if (!hasNext()) {
                    System.out.println(">>> No more cells in path, returning null.");
                    return null; // Hoặc có thể ném ngoại lệ nếu không có vị trí tiếp theo
                }
                return path.get(index);
            }

            public GridCell getNextCell() {
                if (!hasNext()) {
                    System.out.println(">>> No more cells in path, returning null.");
                    return null; // Hoặc có thể ném ngoại lệ nếu không có vị trí tiếp theo
                }
                return path.get(index++);
            }
        }
    }

    /**
     * Clean up old entries from rate limiter cache to prevent memory leaks
     * This method should be called periodically by a scheduler
     */
    public void cleanupRateLimiterCache() {
        long currentTime = System.currentTimeMillis();
        long cleanupThreshold = currentTime - (MIN_MOVE_TARGET_INTERVAL * 1000); // Remove entries older than 100 seconds
        
        lastMoveTargetTime.entrySet().removeIf(entry -> entry.getValue() < cleanupThreshold);
        
        // Also cleanup failed pathfinding counts that are older than the cooldown period
        long failCleanupThreshold = currentTime - (FAILED_ATTEMPT_COOLDOWN * 2); // Double the cooldown period
        failedPathfindingCount.entrySet().removeIf(entry -> {
            String playerKey = entry.getKey();
            Long lastFailTime = lastMoveTargetTime.get(playerKey + ":fail");
            return lastFailTime != null && lastFailTime < failCleanupThreshold;
        });
    }

    /**
     * Clear all move targets for a specific game (e.g., when game ends)
     */
    public void clearGameMoveTargets(String gameId) {
        moveTargets.remove(gameId);
        
        // Also clean up rate limiter entries for this game
        lastMoveTargetTime.entrySet().removeIf(entry -> entry.getKey().startsWith(gameId + ":"));
        
        // Clean up failed pathfinding counts for this game
        failedPathfindingCount.entrySet().removeIf(entry -> entry.getKey().startsWith(gameId + ":"));
    }


    // Inner class để lưu trữ dữ liệu vị trí
    @Data
    public static class PositionData {
        private final Vector2 position;
        private final float speed;
        private final long timestamp;
    }
}
