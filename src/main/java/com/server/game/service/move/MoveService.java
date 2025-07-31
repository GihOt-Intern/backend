package com.server.game.service.move;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.move.MoveService.MoveTarget.PathComponent;
import com.server.game.service.position.PositionService2;
import com.server.game.util.ThetaStarPathfinder;

import lombok.Data;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MoveService {
    
    @Autowired
    private PositionService2 positionService;

    @Autowired
    @Lazy
    private GameCoordinator gameCoordinator;


    private final Map<Entity, MoveTarget> moveTargets = new ConcurrentHashMap<>();

    // Minimum distance threshold to avoid unnecessary pathfinding
    private static final float MIN_MOVE_DISTANCE = 0.5f;
    
    // Service-level rate limiting - Reduced to allow more responsive movement
    private static final long MIN_MOVE_TARGET_INTERVAL = 50; // 50ms = max 20 updates per second
    private final Map<String, Long> lastMoveTargetTime = new ConcurrentHashMap<>();
    
    // Track failed pathfinding attempts to prevent repeated invalid requests
    private final Map<String, Integer> failedPathfindingCount = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5; // Increased threshold
    private static final long FAILED_ATTEMPT_COOLDOWN = 300; // Reduced cooldown

    private String getMappingKeyFor(Entity entity) {
        return entity.getGameId() + ":" + entity.getStringId();
    }

    private String getFailedMappingKeyFor(Entity entity) {
        return entity.getGameId() + ":" + entity.getStringId() + ":fail";
    }

    private String getFailedMappingKeyFor(String mappingKey) {
        return mappingKey + ":fail";
    }

    private boolean isKeyEligibleForCleaning(String key, GameState gameState) {
        return key.startsWith(gameState.getGameId() + ":");
    }

    /**
     * Đặt mục tiêu di chuyển mới cho người chơi
     */
    public void setMove(Entity entity, Vector2 targetPosition) {
        Long currentTime = System.currentTimeMillis();

        try {
            this.preCheckConditions(entity, currentTime);
        } catch (Exception e) {
            log.error("Pre-check conditions failed for entity {}: {}", entity.getStringId(), e.getMessage());
            return;
        }


        float entitySpeed = entity.getSpeed();
        
        // Try to optimize the target position retrieval by
        // getting the most up-to-date position using the existing method
        // if the entity is moving (cannot optimize), use the current position
        Vector2 startPosition = getCurrentRealTimePosition(entity);
    
        // Check if the move distance is too small to avoid unnecessary calculations
        float moveDistance = startPosition.distance(targetPosition);
        if (moveDistance < MIN_MOVE_DISTANCE) {
            log.debug("Move distance {} is too small for entity {}, ignoring", moveDistance, entity.getStringId());
            return;
        }

        GameMapGrid gameMapGrid = entity.getGameMapGrid();

        GameState gameState = entity.getGameState();
        GridCell startCell = gameState.toGridCell(startPosition);
        GridCell targetCell = gameState.toGridCell(targetPosition);

        log.info("Setting move target for entity {}: from {} to {}", entity.getStringId(), startPosition, targetPosition);
        log.info("Calculating path for entity {} from cell {} to cell {}", entity.getStringId(), startCell, targetCell);

        List<GridCell> path = ThetaStarPathfinder.findPath(gameMapGrid.getGrid(), startCell, targetCell);
        
        // Check if pathfinding failed or returned empty path
        if (path == null || path.isEmpty()) {
            log.warn("Pathfinding failed for entity {}:{} from {} to {}", 
                entity.getGameId(), entity.getStringId(), startPosition, targetPosition);

            this.trackFailedPathfinding(entity, currentTime);

            // Return early to prevent further processing
            return;
        }
        
        // Reset failure count on successful pathfinding
        failedPathfindingCount.remove(entity.getStringId());

        PathComponent pathComponent = new PathComponent(path);

        MoveTarget target = new MoveTarget(
            startPosition,
            targetPosition,
            currentTime,
            entitySpeed,
            pathComponent
        );

        // Save and overwrite new target (if any)
        moveTargets.put(entity, target);
    }

    private void preCheckConditions(Entity entity, Long currentTime) {

        this.checkInRateLimit(entity, currentTime);
        this.checkRepeatedFailedPathfindingAttempts(entity, currentTime);


        lastMoveTargetTime.put(entity.getStringId(), currentTime);
    }


    private void checkInRateLimit(Entity entity, long currentTime) {
        // Service-level rate limiting
        Long lastTime = lastMoveTargetTime.get(
            this.getMappingKeyFor(entity));

        if (lastTime != null && (currentTime - lastTime) < MIN_MOVE_TARGET_INTERVAL) {
            log.debug("Service-level rate limit exceeded for entity {} - ignoring move request", entity.getStringId());
            throw new IllegalStateException("Service-level rate limit exceeded for entity " + entity.getStringId());
        }
    }


    private void checkRepeatedFailedPathfindingAttempts(Entity entity, long currentTime) {
        // Check for repeated failed pathfinding attempts
        Integer failCount = failedPathfindingCount.get(this.getMappingKeyFor(entity));
        if (failCount != null && failCount >= MAX_FAILED_ATTEMPTS) {
            Long lastFailTime = lastMoveTargetTime.get(this.getFailedMappingKeyFor(entity));
            if (lastFailTime != null && (currentTime - lastFailTime) < FAILED_ATTEMPT_COOLDOWN) {
                log.debug("Entity {} in cooldown due to repeated failed pathfinding attempts", entity.getStringId());
                throw new IllegalStateException("Entity " + entity.getStringId() + " is in cooldown due to repeated failed pathfinding attempts");
            } else {
                // Reset failure count after cooldown
                failedPathfindingCount.remove(this.getMappingKeyFor(entity));
            }
        }
    }

    private void trackFailedPathfinding(Entity entity, long currentTime) {
        String mappingKey = this.getMappingKeyFor(entity);
        String failMappingKey = this.getFailedMappingKeyFor(entity);
        int currentFailCount = failedPathfindingCount.getOrDefault(mappingKey, 0);
        currentFailCount++;

        failedPathfindingCount.put(mappingKey, currentFailCount);
        lastMoveTargetTime.put(failMappingKey, currentTime);

        if (currentFailCount >= MAX_FAILED_ATTEMPTS) {
            log.warn("Entity {} has reached max failed pathfinding attempts ({}), entering cooldown",
                mappingKey, currentFailCount);
        }
    }

    /**
     * Cập nhật vị trí dựa trên thời gian và mục tiêu di chuyển
     * Được gọi mỗi lần trước khi broadcast vị trí
     */
    public void updatePositions(GameState gameState) {
        for (Entity entity : gameState.getEntities()) {
            this.updatePositions(entity);
        }
    }


    public void updatePositions(Entity entity) {
        MoveTarget target = moveTargets.get(entity);
        if (target == null) {
            log.debug("No move target found for entity {}", entity.getStringId());
            return; // No move target set, nothing to update
        }

        // Function pointer
        Function<GridCell, Vector2> toPosition = cell -> {
            return entity.getGameState().toPosition(cell);
        };

        long currentTime = System.currentTimeMillis();


        float elapsedTime = (currentTime - target.getStartTime()) / 1000.0f;
        float targetSpeed = target.getSpeed();

        Vector2 position = target.getCurrentPosition();
        float totalDistanceCovered = targetSpeed * elapsedTime;
        float remainingDistance = totalDistanceCovered;

        boolean reachedFinalDestination = false;
        while (target.path.hasNext() && !reachedFinalDestination) {
            GridCell nextCell = target.peekNextCell();
            Vector2 nextPosition = toPosition.apply(nextCell);

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
            entity,
            position,
            targetSpeed,
            currentTime
        );

        if (reachedFinalDestination || !target.path.hasNext()) {
            moveTargets.remove(entity);
        }
    }

    public void pushMoveTarget(Entity entity) {
        
    }

    public void popMoveTarget(Entity entity) {
        moveTargets.remove(entity);
    }
    

    /**
     * Get the current real-time position of entity (whether moving or not)
     */
    public Vector2 getCurrentRealTimePosition(Entity entity) {
        // First check if entity is currently moving
        MoveTarget currentTarget = moveTargets.get(entity);
        if (currentTarget != null) {
            // Entity is moving - calculate current real-time position
            GameState gameState = entity.getGameState();

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
        }
     
        // Player is not moving - try to get the most recent position
        // First check pending position (most up-to-date)
        PositionData pendingPos = positionService.getPendingPlayerPosition(entity);
        if (pendingPos != null) {
            return pendingPos.getPosition();
        }
        
        // If no pending position, get current entity position
        return entity.getCurrentPosition();
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
            String failMappingKey = this.getFailedMappingKeyFor(entry.getKey());
            Long lastFailTime = lastMoveTargetTime.get(failMappingKey);
            return lastFailTime != null && lastFailTime < failCleanupThreshold;
        });
    }



    /**
     * Clear all move targets for a specific game (e.g., when game ends)
     */
    public void clearGameMoveTargets(GameState gameState) {
        moveTargets.entrySet().removeIf(entry -> entry.getKey().getGameState().equals(gameState));
        
        // Also clean up rate limiter entries for this game
        lastMoveTargetTime.entrySet().removeIf(entry -> 
            this.isKeyEligibleForCleaning(entry.getKey(), gameState));

        // Clean up failed pathfinding counts for this game
        failedPathfindingCount.entrySet().removeIf(entry -> 
            this.isKeyEligibleForCleaning(entry.getKey(), gameState));
    }


    // Inner class để lưu trữ dữ liệu vị trí
    @Data
    public static class PositionData {
        private final Vector2 position;
        private final float speed;
        private final long timestamp;
    }
}
