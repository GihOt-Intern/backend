package com.server.game.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.game.map.component.Vector2;
import com.server.game.netty.receiveMessageHandler.PositionHandler.PositionData;
import com.server.game.resource.service.GameMapService;

import lombok.Data;

@Service
public class MoveService {
    
    @Autowired
    private PositionService positionService;

    @Autowired
    private GameMapService gameMapService;

    private final Map<String, Map<Short, MoveTarget>> moveTargets = new ConcurrentHashMap<>();


    /**
     * Đặt mục tiêu di chuyển mới cho người chơi
     */
    public void setMoveTarget(String gameId, short slot, Vector2 targetPosition, float speed) {
        PositionData currentPos = positionService.getPlayerPosition(gameId, slot);
        
        if (currentPos == null) {
            currentPos = new PositionData(targetPosition, 
            speed,
            System.currentTimeMillis());

            positionService.updatePendingPosition(
                gameId, 
                slot, 
                targetPosition,
                speed,
                System.currentTimeMillis()
            );
            return;
        }



        // Điều chỉnh vị trí mục tiêu không vượt ra biên của map

        // TODO: need to add arg to get current game map

        Vector2 startPosition = currentPos.getPosition();

        Vector2 targetAdj = null;
        
        // gameMapService.adjustTargetToMap(startPosition, targetPosition);

        targetPosition = targetAdj != null ? targetAdj : targetPosition;

        


        MoveTarget target = new MoveTarget(
            startPosition,
            targetPosition,
            speed,
            System.currentTimeMillis()
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

        long currentTime = System.currentTimeMillis();

        for (Map.Entry<Short, MoveTarget> entry : targets.entrySet()) {
            short slot = entry.getKey();
            MoveTarget target = entry.getValue();

            float elapsedTime = (currentTime - target.getStartTime()) / 1000.0f;

            Vector2 startPosition = target.getStartPosition();
            Vector2 targetPosition = target.getTargetPosition();
            Vector2 dPosition = targetPosition.subtract(startPosition);
            float distance = dPosition.length();

            float timeToTarget = distance / target.getSpeed();

            if (elapsedTime >= timeToTarget) {
                positionService.updatePendingPosition(
                    gameId, 
                    slot, 
                    target.getTargetPosition(), 
                    target.getSpeed(),
                    currentTime
                );

                //Xoá mục tiêu
                targets.remove(slot);
            } else {

                float ratio = elapsedTime / timeToTarget;
                Vector2 dTarget = dPosition.multiply(ratio);
                Vector2 newPosition = startPosition.add(dTarget);

                positionService.updatePendingPosition(gameId, slot, newPosition, target.getSpeed(), currentTime);
            }
        }
    }

    public void clearMoveTargets(String gameId) {
        moveTargets.remove(gameId);
    }

    @Data
    public static class MoveTarget {
        private final Vector2 startPosition;
        private final Vector2 targetPosition;
        private final float speed;
        private final long startTime;
    }
}
