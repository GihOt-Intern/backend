package com.server.game.service.position;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.move.MoveService.PositionData;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Deprecated
@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PositionService {

    
    // Cache tạm thời để lưu vị trí mới từ client (chưa broadcast)
    private final Map<Entity, PositionData> pendingPositionCache = new ConcurrentHashMap<>();
    
    /**
     * Cập nhật vị trí của player vào pending cache (chưa broadcast)
     */
    public void updatePendingPosition(Entity entity, Vector2 position, float speed, long timestamp) {
        PositionData positionData = new PositionData(position, speed, timestamp);

        // Cập nhật pending cache
        pendingPositionCache.put(entity, positionData);
    }
    
    

    public Map<Entity, PositionData> deepCopyPendingPositionsOf(GameState gameState) {
        return new ConcurrentHashMap<>(pendingPositionCache.entrySet()
            .stream()
            .filter(entry -> entry.getKey().getGameState().equals(gameState))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
    
    
    /**
     * Lấy pending position cho một entity cụ thể (vị trí mới nhất)
     */
    public PositionData getPendingPlayerPosition(Entity entity) {
        return pendingPositionCache.get(entity);
    }
    

    public void popPendingPosition(Entity entity) {
        pendingPositionCache.remove(entity);
    }
    
    /**
     * Xóa dữ liệu vị trí khi game kết thúc
     */
    public void clearPendingPositionsOf(GameState gameState) {
        pendingPositionCache.entrySet()
            .removeIf(entry -> entry.getKey().getGameState().equals(gameState));
    }
} 