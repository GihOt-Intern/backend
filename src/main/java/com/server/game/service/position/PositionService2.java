package com.server.game.service.position;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.move.MoveService.PositionData;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PositionService2 {

    GameStateService gameStateService;
    
    
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
    
    // /**
    //  * Lấy vị trí của tất cả player trong game
    //  */
    // public Map<Short, PositionData> getGamePositions(String gameId) {
    //     // Thử lấy từ cache trước
    //     Map<Short, PositionData> cached = positionCache.get(gameId);
    //     if (cached != null && !cached.isEmpty()) {
    //         return cached;
    //     }
        
    //     // Nếu không có trong cache, load từ Redis
    //     Map<Short, PositionData> positions = new ConcurrentHashMap<>();
    //     try {
    //         for (short slot = 0; slot < 4; slot++) {
    //             String key = POSITION_KEY_PREFIX + gameId + ":" + slot;
    //             PositionData position = redisUtil.get(key, PositionData.class);
    //             if (position != null) {
    //                 positions.put(slot, position);
    //             }
    //         }
    //     } catch (Exception e) {
    //         // Handle exception (e.g., log error)
    //         e.printStackTrace();
    //         return positions; // Trả về positions rỗng nếu có lỗi
    //     }
        
    //     // Cập nhật cache
    //     positionCache.put(gameId, positions);
    //     return positions;
    // }
    
    /**
     * Lấy vị trí của một player cụ thể
     */
    @SuppressWarnings("unused")
    @Deprecated
    public PositionData getPlayerPosition(String gameId, short slot) {
        // Thử lấy từ cache trước
        // Map<Short, PositionData> gamePositions = positionCache.get(gameId);
        // if (gamePositions != null) {
        //     PositionData cached = gamePositions.get(slot);
        //     if (cached != null) {
        //         return cached;
        //     }
        // }
        
        // // Nếu không có trong cache, load từ Redis
        // String key = POSITION_KEY_PREFIX + gameId + ":" + slot;
        // try {
        //     PositionData position = redisUtil.get(key, PositionData.class);
        //     if (position != null) {
        //         // Cập nhật cache
        //         positionCache.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
        //                     .put(slot, position);
        //     }
        //     return position;
        // } catch (Exception e) {
            return null;
        // }
    }
    
    /**
     * Lấy pending position cho một entity cụ thể (vị trí mới nhất)
     */
    public PositionData getPendingPlayerPosition(Entity entity) {
        return pendingPositionCache.get(entity);
    }
    
    /**
     * Lấy pending positions để broadcast
     */
    @SuppressWarnings("unused")
    @Deprecated
    public Map<Entity, PositionData> getPendingPositions(String gameId) {
        // return pendingPositionCache.getOrDefault(gameId, new ConcurrentHashMap<>());
        return null;
    }
    
    /**
     * Xóa pending positions sau khi đã broadcast
     */
    @SuppressWarnings("unused")
    @Deprecated
    public void clearPendingPositions(String gameId) {
        // pendingPositionCache.remove(gameId);
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
    
    /**
     * Kiểm tra xem vị trí có hợp lệ không (chống hack)
     */
    @SuppressWarnings("unused")
    @Deprecated
    public boolean isValidPosition(String gameId, short slot, Vector2 position) {
        PositionData lastPosition = getPlayerPosition(gameId, slot);
        if (lastPosition == null) {
            return true; // Lần đầu di chuyển
        }
        
        // Giới hạn tốc độ di chuyển (ví dụ: 10 đơn vị/tick)
        return true;
    }

} 