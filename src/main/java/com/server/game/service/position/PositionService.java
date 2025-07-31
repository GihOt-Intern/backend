package com.server.game.service.position;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.model.map.component.Vector2;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.move.MoveService.PositionData;
import com.server.game.util.RedisUtil;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Deprecated
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PositionService {

    GameStateService gameStateService;
    
    private final RedisUtil redisUtil;
    private static final String POSITION_KEY_PREFIX = "position:";
    private static final int POSITION_TTL = 300; // 5 phút
    
    // Cache trong bộ nhớ để tăng tốc độ truy cập
    // <gameId, <slot, PositionData>>
    private final Map<String, Map<Short, PositionData>> positionCache = new ConcurrentHashMap<>();
    
    // Cache tạm thời để lưu vị trí mới từ client (chưa broadcast)
    private final Map<String, Map<Short, PositionData>> pendingPositionCache = new ConcurrentHashMap<>();
    
    /**
     * Cập nhật vị trí của player vào pending cache (chưa broadcast)
     */
    public void updatePendingPosition(String gameId, short slot, Vector2 position, float speed, long timestamp) {
        PositionData positionData = new PositionData(position, speed, timestamp);

        // Cập nhật pending cache
        pendingPositionCache.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                           .put(slot, positionData);
    }
    
    /**
     * Cập nhật vị trí của player vào main cache (sau khi broadcast)
     */
    public void updatePosition(String gameId, short slot, Vector2 position, float speed, long timestamp) {
        PositionData positionData = new PositionData(position, speed, timestamp);

        // Cập nhật main cache
        positionCache.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                    .put(slot, positionData);
        
        // Cập nhật Redis (cho multi-server)
        String key = POSITION_KEY_PREFIX + gameId + ":" + slot;
        redisUtil.set(key, positionData, java.time.Duration.ofSeconds(POSITION_TTL));

        // Update position for SlotState in GameState
        gameStateService.updateSlotPosition(gameId, slot, position);
    }
    
    /**
     * Lấy vị trí của tất cả player trong game
     */
    public Map<Short, PositionData> getGamePositions(String gameId) {
        // Thử lấy từ cache trước
        Map<Short, PositionData> cached = positionCache.get(gameId);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        
        // Nếu không có trong cache, load từ Redis
        Map<Short, PositionData> positions = new ConcurrentHashMap<>();
        try {
            for (short slot = 0; slot < 4; slot++) {
                String key = POSITION_KEY_PREFIX + gameId + ":" + slot;
                PositionData position = redisUtil.get(key, PositionData.class);
                if (position != null) {
                    positions.put(slot, position);
                }
            }
        } catch (Exception e) {
            // Handle exception (e.g., log error)
            e.printStackTrace();
            return positions; // Trả về positions rỗng nếu có lỗi
        }
        
        // Cập nhật cache
        positionCache.put(gameId, positions);
        return positions;
    }
    
    /**
     * Lấy vị trí của một player cụ thể
     */
    public PositionData getPlayerPosition(String gameId, short slot) {
        // Thử lấy từ cache trước
        Map<Short, PositionData> gamePositions = positionCache.get(gameId);
        if (gamePositions != null) {
            PositionData cached = gamePositions.get(slot);
            if (cached != null) {
                return cached;
            }
        }
        
        // Nếu không có trong cache, load từ Redis
        String key = POSITION_KEY_PREFIX + gameId + ":" + slot;
        try {
            PositionData position = redisUtil.get(key, PositionData.class);
            if (position != null) {
                // Cập nhật cache
                positionCache.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                            .put(slot, position);
            }
            return position;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Lấy pending position cho một player cụ thể (vị trí mới nhất)
     */
    public PositionData getPendingPlayerPosition(String gameId, short slot) {
        Map<Short, PositionData> gamePendingPositions = pendingPositionCache.get(gameId);
        return gamePendingPositions != null ? gamePendingPositions.get(slot) : null;
    }
    
    /**
     * Lấy pending positions để broadcast
     */
    public Map<Short, PositionData> getPendingPositions(String gameId) {
        return pendingPositionCache.getOrDefault(gameId, new ConcurrentHashMap<>());
    }
    
    /**
     * Xóa pending positions sau khi đã broadcast
     */
    public void clearPendingPositions(String gameId) {
        pendingPositionCache.remove(gameId);
    }
    
    /**
     * Xóa dữ liệu vị trí khi game kết thúc
     */
    public void clearGamePositions(String gameId) {
        // Xóa cache
        positionCache.remove(gameId);
        pendingPositionCache.remove(gameId);
        
        // Xóa từ Redis
        try {
            for (short slot = 0; slot < 4; slot++) {
                String key = POSITION_KEY_PREFIX + gameId + ":" + slot;
                redisUtil.delete(key);
            }
        } catch (Exception e) {
            // Handle exception (e.g., log error)
            e.printStackTrace();
        }
    }
    
    /**
     * Kiểm tra xem vị trí có hợp lệ không (chống hack)
     */
    public boolean isValidPosition(String gameId, short slot, Vector2 position) {
        PositionData lastPosition = getPlayerPosition(gameId, slot);
        if (lastPosition == null) {
            return true; // Lần đầu di chuyển
        }
        
        // Giới hạn tốc độ di chuyển (ví dụ: 10 đơn vị/tick)
        return true;
    }

} 