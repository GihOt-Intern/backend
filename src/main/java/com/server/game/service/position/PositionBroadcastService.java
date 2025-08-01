package com.server.game.service.position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.server.game.model.game.GameState;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.PositionSend;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.move.MoveService;
import com.server.game.service.move.MoveService.PositionData;
import com.server.game.model.game.Entity;


import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PositionBroadcastService {
    
    PositionService2 positionService;
    MoveService moveService;
    GameStateService gameStateService;
    
    /**
     * Hủy đăng ký game - được gọi từ GameScheduler
     */
    public void unregisterGame(GameState gameState) {
        positionService.clearPendingPositionsOf(gameState);
        moveService.clearGameMoveTargets(gameState);
        ChannelManager.clearGameSlotMappings(gameState.getGameId());
        log.info("Unregistered game from position broadcasting: {}", gameState.getGameId());
    }
    
    /**
     * Broadcast vị trí cho một game cụ thể - được gọi từ GameScheduler
     */
    public void broadcastGamePositions(GameState gameState) {


        Map<Entity, PositionData> gameStatePendingPositions = 
            positionService.deepCopyPendingPositionsOf(gameState);

        // Filter entities having changed position
        gameStatePendingPositions.entrySet().removeIf(entry -> {
            Entity entity = entry.getKey();
            
            Vector2 oldPosition = entity.getCurrentPosition();
            Vector2 newPosition = entry.getValue().getPosition();
            return !hasPositionChanged(oldPosition, newPosition);
        });

        if (gameStatePendingPositions.isEmpty()) {
            return; // Không có vị trí nào thay đổi
        }

        // Tạo message để broadcast
        long currentTime = System.currentTimeMillis();
        PositionSend positionSend = new PositionSend(gameStatePendingPositions, currentTime);
        
        String gameId = gameState.getGameId();
        log.debug("Broadcasting positions for game {}: {}", gameId, positionSend);

        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(positionSend); // sendTarget of positionSend message is AMatchTarget
        } else {
            // Không có player nào trong game, không cần broadcast, xoá game khỏi activeGames
            this.unregisterGame(gameState);
        }
        
        // Update new positions for entities
        for (Map.Entry<Entity, PositionData> entry : gameStatePendingPositions.entrySet()) {
            Entity entity = entry.getKey();
            Vector2 newPosition = entry.getValue().getPosition();
            entity.updatePosition(newPosition);
        }
        
        // Xóa pending positions sau khi đã broadcast
        positionService.clearPendingPositionsOf(gameState);
    }
    
    /**
     * Kiểm tra xem player có thay đổi vị trí so với lần cập nhật trước không
     */
    private boolean hasPositionChanged(Vector2 oldPosition, Vector2 newPosition) {
        if (oldPosition == null) {
            return true; // Lần đầu, coi như thay đổi
        }
        
        // So sánh vị trí với ngưỡng thay đổi nhỏ
        float threshold = 0.005f; // Ngưỡng thay đổi tối thiểu

        return oldPosition.distance(newPosition) > threshold;
    }
} 