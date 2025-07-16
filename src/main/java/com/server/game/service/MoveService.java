package com.server.game.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.game.netty.receiveMessageHandler.PositionHandler.PositionData;

import lombok.Data;

@Service
public class MoveService {
    
    @Autowired
    private PositionService positionService;

    private final Map<String, Map<Short, MoveTarget>> moveTargets = new ConcurrentHashMap<>();


    /**
     * Đặt mục tiêu di chuyển mới cho người chơi
     */
    public void setMoveTarget(String gameId, short slot, float targetX, float targetY, float speed) {
        PositionData currentPos = positionService.getPlayerPosition(gameId, slot);
        if (currentPos == null) {
            currentPos = new PositionData(targetX, targetY, System.currentTimeMillis());
            positionService.updatePendingPosition(gameId, slot, targetX, targetY, System.currentTimeMillis());
            return;
        }

        MoveTarget target = new MoveTarget(
            currentPos.getX(), currentPos.getY(),
            targetX, targetY,
            speed,
            System.currentTimeMillis()
        );

        // Lưu mục tiêu
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

        for (Map.Entry<Short,MoveTarget> entry : targets.entrySet()) {
            short slot = entry.getKey();
            MoveTarget target = entry.getValue();

            float elapsedTime = (currentTime - target.getStartTime()) / 1000.0f;

            float dx = target.getTargetX() - target.getStartX();
            float dy = target.getTargetY() - target.getStartY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            float timeToTarget = distance / target.getSpeed();

            if (elapsedTime >= timeToTarget) {
                positionService.updatePendingPosition(gameId, slot, target.getTargetX(), target.getTargetY(), currentTime);

                //Xoá mục tiêu
                targets.remove(slot);
            } else {
                float ratio = elapsedTime / timeToTarget;
                float newX = target.getStartX() + dx * ratio;
                float newY = target.getStartY() + dy * ratio;

                positionService.updatePendingPosition(gameId, slot, newX, newY, currentTime);   
            }
        }
    }

    public void clearMoveTargets(String gameId) {
        moveTargets.remove(gameId);
    }

    @Data
    public static class MoveTarget {
        private final float startX;
        private final float startY;
        private final float targetX;
        private final float targetY;
        private final float speed;
        private final long startTime;
    }
}
