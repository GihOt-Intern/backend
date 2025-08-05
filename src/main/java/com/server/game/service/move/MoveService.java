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
import com.server.game.service.attack.AttackService;
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

    @Autowired @Lazy
    private AttackService attackService;


    private final Map<GameState, Map<Entity, MoveTarget>> moveTargets = new ConcurrentHashMap<>();
    
    // Service-level rate limiting - Reduced to allow more responsive movement
    private static final long MIN_MOVE_TARGET_INTERVAL = 75; // 50ms = max 20 updates per second
    private final Map<GameState, Map<String, Long>> lastMoveTargetTimestamp = new ConcurrentHashMap<>();

    // Track failed pathfinding attempts to prevent repeated invalid requests
    private final Map<GameState, Map<String, Integer>> failedPathfindingCount = new ConcurrentHashMap<>();
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


//******* MAPS PRIVATE INTERFACE *********//
    private void pushMoveTarget2Map(Entity entity, MoveTarget target) {
        // Push the move target to the map
        GameState gameState = entity.getGameState();
        Map<Entity, MoveTarget> gameMoveTargets = 
            moveTargets.computeIfAbsent(gameState, k -> new ConcurrentHashMap<>());
        gameMoveTargets.put(entity, target);
    }

    private MoveTarget peekMoveTargetFromMap(Entity entity) {
        // Get the move target from the map
        GameState gameState = entity.getGameState();
        Map<Entity, MoveTarget> gameMoveTargets = moveTargets.get(gameState);
        if (gameMoveTargets == null) {
            return null;
        }
        return gameMoveTargets.get(entity);
    }

    private void removeMoveTargetFromMap(Entity entity) {
        GameState gameState = entity.getGameState();
        Map<Entity, MoveTarget> gameMoveTargets = moveTargets.get(gameState);
        if (gameMoveTargets != null) {
            gameMoveTargets.remove(entity);
        }
        if (gameMoveTargets == null || gameMoveTargets.isEmpty()) {
            moveTargets.remove(gameState);
        }
    }

    

    private void pushLastMoveTargetTimestamp(Entity entity, long currentTime) {
        GameState gameState = entity.getGameState();
        Map<String, Long> lastMoveTargetTime = 
            this.lastMoveTargetTimestamp.computeIfAbsent(
                gameState, k -> new ConcurrentHashMap<>());
        String mappingKey = this.getMappingKeyFor(entity);
        lastMoveTargetTime.put(mappingKey, currentTime);
    }

    private Long peekLastMoveTargetTimestamp(Entity entity) {
        GameState gameState = entity.getGameState();
        Map<String, Long> lastMoveTargetTime = this.lastMoveTargetTimestamp.get(gameState);
        if (lastMoveTargetTime == null) {
            return null;
        }
        String mappingKey = this.getMappingKeyFor(entity);
        return lastMoveTargetTime.get(mappingKey);
    }
    
    private Long peekFailedLastMoveTargetTimestamp(GameState gameState, String keyMapping) {
        Map<String, Long> gameLastMoveTargetTimestamp = lastMoveTargetTimestamp.get(gameState);
        if (gameLastMoveTargetTimestamp == null) { return null; }
        String failedMappingKey = this.getFailedMappingKeyFor(keyMapping);
        return gameLastMoveTargetTimestamp.get(failedMappingKey);
    }

    private void cleanupOutdatedLastMoveTargetTimestamps(long cleanupThreshold) {
        lastMoveTargetTimestamp.entrySet().removeIf(entry -> {
            Map<String, Long> gameMoveTargets = entry.getValue();

            gameMoveTargets.entrySet().removeIf(moveEntry -> {
                Long timestamp = moveEntry.getValue();
                return timestamp < cleanupThreshold;
            });

            // Remove empty gameMoveTargets map
            return gameMoveTargets.isEmpty();
        });
    }

    private void cleanupOutdatedFailedPathfindingCounts(long cleanupThreshold) {
        failedPathfindingCount.entrySet().removeIf(entry -> {
            GameState gameState = entry.getKey();
            Map<String, Integer> failCounts = entry.getValue();

            failCounts.entrySet().removeIf(failEntry -> {
                String mappingKey = failEntry.getKey();
                Long lastFailTime = this.peekFailedLastMoveTargetTimestamp(gameState, mappingKey);
                return lastFailTime != null && lastFailTime < cleanupThreshold;
            });

            return failCounts.isEmpty();
        });
    }
    
    private void pushFailedLastMoveTargetTime(Entity entity, long currentTime) {
        GameState gameState = entity.getGameState();
        Map<String, Long> lastMoveTargetTime = 
            this.lastMoveTargetTimestamp.computeIfAbsent(
                gameState, k -> new ConcurrentHashMap<>());
        String mappingKey = this.getFailedMappingKeyFor(entity);
        lastMoveTargetTime.put(mappingKey, currentTime);
    }

    private Long peekFailedLastMoveTargetTime(Entity entity) {
        GameState gameState = entity.getGameState();
        Map<String, Long> failedLastMoveTargetTime = this.lastMoveTargetTimestamp.get(gameState);
        if (failedLastMoveTargetTime == null) {
            return null;
        }
        String failedMappingKey = this.getFailedMappingKeyFor(entity);
        return failedLastMoveTargetTime.get(failedMappingKey);
    }

    private void increaseFailedPathfindingCount(Entity entity) {
        GameState gameState = entity.getGameState();
        Map<String, Integer> failedPathfindingCount = 
            this.failedPathfindingCount.computeIfAbsent(
                gameState, k -> new ConcurrentHashMap<>());
        String mappingKey = this.getMappingKeyFor(entity);
        failedPathfindingCount.put(mappingKey, 
            failedPathfindingCount.getOrDefault(mappingKey, 0) + 1);
    }

    private Integer peekFailedPathfindingCount(Entity entity) {
        GameState gameState = entity.getGameState();
        Map<String, Integer> failedPathfindingCount = this.failedPathfindingCount.get(gameState);
        if (failedPathfindingCount == null) {
            return 0; // No failed attempts recorded
        }
        return failedPathfindingCount.getOrDefault(this.getMappingKeyFor(entity), 0);
    }

    private void removeFailedPathfindingCount(Entity entity) {
        GameState gameState = entity.getGameState();
        Map<String, Integer> failedPathfindingCount = this.failedPathfindingCount.get(gameState);
        if (failedPathfindingCount != null) {
            failedPathfindingCount.remove(this.getMappingKeyFor(entity));
            if (failedPathfindingCount.isEmpty()) {
                this.failedPathfindingCount.remove(gameState);
            }
        }
    }
//******* END MAPS PRIVATE INTERFACE *********//

    /**
     * Đặt mục tiêu di chuyển mới cho người chơi
     */
    public void setMove(Entity entity, Vector2 targetPosition, boolean needStopAttack) {
        Long currentTime = System.currentTimeMillis();

        boolean preCheckPassed = this.preCheckConditions(entity, currentTime);
        if (!preCheckPassed) {
            return;
        }

        System.out.println(">>> [Log in MoveService.setMove] preCheckPassed");

        if (needStopAttack) {
            attackService.stopAttack(entity);
        }

        // If prechecks ok, update last move target time
        this.pushLastMoveTargetTimestamp(entity, currentTime);


        float entitySpeed = entity.getSpeed();
        
        // Try to optimize the target position retrieval by
        // getting the most up-to-date position using the existing method
        // if the entity is moving (cannot optimize), use the current position
        Vector2 startPosition = getCurrentRealTimePosition(entity);
    
        // Check if the move distance is too small to avoid unnecessary calculations
        // float moveDistance = startPosition.distance(targetPosition);
        // if (moveDistance < MIN_MOVE_DISTANCE) {
        //     log.info("Move distance {} is too small for entity {}, ignoring", moveDistance, entity.getStringId());
        //     return;
        // }

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
        
        // Reset failure count on successful pathfinding (by removing it)
        this.removeFailedPathfindingCount(entity);

        PathComponent pathComponent = new PathComponent(path);

        MoveTarget target = new MoveTarget(
            startPosition,
            targetPosition,
            currentTime,
            entitySpeed,
            pathComponent
        );

        // Save and overwrite new target (if any)
        this.pushMoveTarget2Map(entity, target);
    }

    private boolean preCheckConditions(Entity entity, Long currentTime) {
        return this.checkInRateLimit(entity, currentTime) &&
        this.checkRepeatedFailedPathfindingAttempts(entity, currentTime);
    }


    private boolean checkInRateLimit(Entity entity, long currentTime) {
        // Service-level rate limiting
        Long lastTime = this.peekLastMoveTargetTimestamp(entity);

        if (lastTime != null && (currentTime - lastTime) < MIN_MOVE_TARGET_INTERVAL) {
            log.info("Service-level rate limit exceeded for entity {} - ignoring move request", entity.getStringId());
            return false; // Ignore move request if rate limit exceeded
        }
        return true;
    }

    private boolean checkRepeatedFailedPathfindingAttempts(Entity entity, long currentTime) {
        // Check for repeated failed pathfinding attempts
        Integer failCount = this.peekFailedPathfindingCount(entity);
        if (failCount != null && failCount >= MAX_FAILED_ATTEMPTS) {
            Long lastFailTime = this.peekFailedLastMoveTargetTime(entity);
            if (lastFailTime != null && (currentTime - lastFailTime) < FAILED_ATTEMPT_COOLDOWN) {
                log.info("Entity {} in cooldown due to repeated failed pathfinding attempts", entity.getStringId());
                return false; 
                // throw new IllegalStateException("Entity " + entity.getStringId() + " is in cooldown due to repeated failed pathfinding attempts");
            } else {
                // Reset failure count after cooldown (by removing it)
                this.removeFailedPathfindingCount(entity);
                return true;
            }
        }
        return true;
    }

    private void trackFailedPathfinding(Entity entity, long currentTime) {
        int currentFailCount = this.peekFailedPathfindingCount(entity);

        this.increaseFailedPathfindingCount(entity);
        this.pushFailedLastMoveTargetTime(entity, currentTime);

        if (currentFailCount >= MAX_FAILED_ATTEMPTS) {
            log.warn("Entity {} has reached max failed pathfinding attempts ({}), entering cooldown",
                entity.getStringId(), currentFailCount);
        }
    }

    /**
     * Cập nhật vị trí dựa trên thời gian và mục tiêu di chuyển
     * Được gọi mỗi lần trước khi broadcast vị trí
     */
    public void updatePositions(GameState gameState) {
        for (Entity entity : gameState.getEntities()) {
            this.updatePositionOf(entity);
        }
    }


    private void updatePositionOf(Entity entity) {
        MoveTarget target = this.peekMoveTargetFromMap(entity);
        if (target == null) {
            log.debug("No move target found for entity {}", entity.getStringId());
            return; // No move target set, nothing to update
        }

        // Lambda function
        Function<GridCell, Vector2> toPosition = cell -> {
            return entity.getGameState().toPosition(cell);
        };

        long currentTime = System.currentTimeMillis();


        float elapsedTime = (currentTime - target.getStartTime()) / 1000.0f;
        float targetSpeed = target.getSpeed();

        Vector2 position = target.getCurrentPosition();
        float totalDistanceCovered = targetSpeed * elapsedTime;
        float remainingDistance = totalDistanceCovered;

        // TODO: need refactor
        // if (entity.getAttackContext() != null) {
        //     float attackRange = entity.getAttackRange();
        //     float distanceToTarget = entity.getCurrentPosition()
        //         .distance(entity.getAttackContext().getTarget().getCurrentPosition());
        //     if (distanceToTarget > attackRange) {
        //         remainingDistance = Math.min(remainingDistance, distanceToTarget - attackRange);
        //     } else {
        //         remainingDistance = 0; 
        //     }
        // }

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

        // System.out.println(">>> [Log in MoveService.updatePositions] Updated pending position for entity: " + 
        //     entity.getStringId() + ", new position: " + position + ", speed: " + targetSpeed +
        //     " in gameId=" + entity.getGameId());

        if (reachedFinalDestination || !target.path.hasNext()) {
            this.removeMoveTargetFromMap(entity);
        }
    }

    /**
     * Get the current real-time position of entity (whether moving or not)
     */
    private Vector2 getCurrentRealTimePosition(Entity entity) {
        // First check if entity is currently moving
        MoveTarget currentTarget = this.peekMoveTargetFromMap(entity);
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
        
        this.cleanupOutdatedLastMoveTargetTimestamps(cleanupThreshold);
        
        // Also cleanup failed pathfinding counts that are older than the cooldown period
        long failCleanupThreshold = currentTime - (FAILED_ATTEMPT_COOLDOWN * 2); // Double the cooldown period
        
        this.cleanupOutdatedFailedPathfindingCounts(failCleanupThreshold);
    }



    /**
     * Clear all move targets for a specific game (e.g., when game ends)
     */
    public void clearGameMoveTargets(GameState gameState) {
        moveTargets.remove(gameState); 
        
        // Also clean up rate limiter entries for this game
        lastMoveTargetTimestamp.remove(gameState);

        // Clean up failed pathfinding counts for this game
        failedPathfindingCount.remove(gameState);
    }


    // Inner class để lưu trữ dữ liệu vị trí
    @Data
    public static class PositionData {
        private final Vector2 position;
        private final float speed;
        private final long timestamp;
    }
}
