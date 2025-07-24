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
import com.server.game.resource.model.GameMapGrid;
import com.server.game.service.MoveService.MoveTarget.PathComponent;

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

                    log.info(">>> Slot {} moved to next cell: {}, remaining distance: {}", 
                        slot, nextCell, remainingDistance);

                    if (!target.path.hasNext()) {
                        reachedFinalDestination = true;
                        log.info(">>> Slot {} reached final destination: {}", slot, nextCell);
                    }
                } else {
                    Vector2 direction = nextPosition.subtract(position).normalize();
                    position = position.add(direction.multiply(remainingDistance));

                    log.info(">>> Slot {} moved partially to: {}, remaining distance: {}", 
                        slot, nextPosition, remainingDistance);
                    
                    break;
                }
            }

            log.info(">>> Updating position for slot {}: {}", slot, position);
            positionService.updatePendingPosition(
                gameId,
                slot,
                position,
                targetSpeed,
                currentTime
            );

            if (reachedFinalDestination || !target.path.hasNext()) {
                log.info(">>> Slot {} has reached the final destination or path is complete, clearing target.", slot);
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
