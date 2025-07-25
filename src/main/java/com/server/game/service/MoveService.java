package com.server.game.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.server.game.map.AStarPathfinder;
import com.server.game.map.component.GridCell;
import com.server.game.map.component.Vector2;
import com.server.game.model.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.resource.service.ChampionService;
import com.server.game.service.MoveService.MoveTarget.PathComponent;
import com.server.game.util.ChampionEnum;

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

    @Autowired
    private ChampionService championService;


    private final Map<String, Map<Short, MoveTarget>> moveTargets = new ConcurrentHashMap<>();


    /**
     * Đặt mục tiêu di chuyển mới cho người chơi
     */
    public void setMoveTarget(String gameId, short slot, Vector2 targetPosition) {
        
        GameState gameState = gameCoordinator.getGameState(gameId);

        float slotSpeed = gameState.getSpeed(slot);
        
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


        

        GameMapGrid gameMapGrid = gameState.getGameMapGrid();

        Vector2 startPosition = currentPos.getPosition();
        GridCell startCell = gameState.toGridCell(startPosition);
        GridCell targetCell = gameState.toGridCell(targetPosition);

        log.info("Setting move target for slot {}: from {} to {}", slot, startPosition, targetPosition);
        log.info("Calculating path for slot {} from cell {} to cell {}", slot, startCell, targetCell);

        List<GridCell> path = AStarPathfinder.findPath(gameMapGrid.getGrid(), startCell, targetCell);
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

    private static final float PATH_RECALCULATION_THRESHOLD = 1.5f; // Distance in grid units
    private static final long PATH_RECALCULATION_COOLDOWN = 300; // Milliseconds
    private final Map<String, Map<Short, Long>> lastPathCalculationTime = new ConcurrentHashMap<>();

    /**
     * Set move target for combat - optimized for frequent updates
     */
    public void setCombatMoveTarget(String gameId, short attackerSlot, short targetSlot) {
        GameState gameState = gameCoordinator.getGameState(gameId);
        PositionData attackerPos = positionService.getPlayerPosition(gameId, attackerSlot);
        PositionData targetPos = positionService.getPlayerPosition(gameId, targetSlot);
        
        if (attackerPos == null || targetPos == null) {
            return;
        }
        
        // Get current time
        long currentTime = System.currentTimeMillis();
        
        // Check if we're still in cooldown period
        Map<Short, Long> lastCalcTimes = lastPathCalculationTime.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        Long lastCalcTime = lastCalcTimes.get(attackerSlot);
        if (lastCalcTime != null && (currentTime - lastCalcTime) < PATH_RECALCULATION_COOLDOWN) {
            log.debug("Path recalculation on cooldown for slot {}", attackerSlot);
            return;
        }
        
        // Check if target has moved significantly
        MoveTarget currentTarget = moveTargets.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).get(attackerSlot);
        if (currentTarget != null) {
            Vector2 lastTargetPos = currentTarget.getTargetPosition();
            float targetMoveDist = lastTargetPos.distance(targetPos.getPosition());
            
            if (targetMoveDist < PATH_RECALCULATION_THRESHOLD) {
                log.debug("Target hasn't moved enough to recalculate path");
                return;
            }
        }
        
        // Calculate optimal position to attack from (slightly closer than max attack range)
        ChampionEnum attackerChampion = getChampionForSlot(gameId, attackerSlot);
        if (attackerChampion == null) {
            log.warn("No champion found for slot {} in game {}", attackerSlot, gameId);
            return;
        }
        
        float attackRange = getChampionAttackRange(attackerChampion) * 0.9f; // 90% of max range
        Vector2 direction = attackerPos.getPosition().subtract(targetPos.getPosition()).normalize();
        Vector2 optimalPosition = targetPos.getPosition().add(direction.multiply(attackRange));
        
        // For very close targets, use direct following instead of pathfinding
        if (attackerPos.getPosition().distance(targetPos.getPosition()) < attackRange * 1.5f) {
            // Simple direct movement toward target
            Vector2 directMoveTarget = targetPos.getPosition().add(
                direction.multiply(attackRange * 0.9f)
            );
            
            // Update position directly without pathfinding
            positionService.updatePendingPosition(
                gameId, attackerSlot, directMoveTarget, 
                gameState.getSpeed(attackerSlot), currentTime
            );
            lastCalcTimes.put(attackerSlot, currentTime);
            return;
        }
        
        // Find path to the optimal position
        GameMapGrid gameMapGrid = gameState.getGameMapGrid();
        GridCell startCell = gameState.toGridCell(attackerPos.getPosition());
        GridCell targetCell = gameState.toGridCell(optimalPosition);
        
        List<GridCell> path = AStarPathfinder.findPath(gameMapGrid.getGrid(), startCell, targetCell);
        
        // Set the new target
        MoveTarget target = new MoveTarget(
            attackerPos.getPosition(),
            optimalPosition,
            currentTime,
            gameState.getSpeed(attackerSlot),
            new PathComponent(path)
        );
        
        // Store the target and update last calculation time
        moveTargets.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).put(attackerSlot, target);
        lastCalcTimes.put(attackerSlot, currentTime);
        
        log.info("Combat move target set for slot {} to attack slot {}", attackerSlot, targetSlot);
    }

    /**
     * Get champion enum for a slot
     */
    private ChampionEnum getChampionForSlot(String gameId, short slot) {
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);
        return slot2ChampionId != null ? slot2ChampionId.get(slot) : null;
    }
    
    /**
     * Get champion's attack range
     */
    private float getChampionAttackRange(ChampionEnum championEnum) {
        var champion = championService.getChampionById(championEnum);
        return champion != null ? champion.getAttackRange() : 1.0f; // Default range
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


    // Inner class để lưu trữ dữ liệu vị trí
    @Data
    public static class PositionData {
        private final Vector2 position;
        private final float speed;
        private final long timestamp;
    }
}
