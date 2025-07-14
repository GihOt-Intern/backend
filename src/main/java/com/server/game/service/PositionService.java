package com.server.game.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.netty.receiveMessageHandler.PositionHandler.PositionData;
import com.server.game.util.RedisUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PositionService {
    
    private final RedisUtil redisUtil;
    private static final String POSITION_KEY_PREFIX = "position:";
    private static final int POSITION_TTL = 300; // 5 phút
    
    // Cache trong bộ nhớ để tăng tốc độ truy cập
    private final Map<String, Map<Short, PositionData>> positionCache = new ConcurrentHashMap<>();
    
    // Cache tạm thời để lưu vị trí mới từ client (chưa broadcast)
    private final Map<String, Map<Short, PositionData>> pendingPositionCache = new ConcurrentHashMap<>();
    
    /**
     * Cập nhật vị trí của player vào pending cache (chưa broadcast)
     */
    public void updatePendingPosition(String gameId, short slot, float x, float y, long timestamp) {
        PositionData positionData = new PositionData(x, y, timestamp);
        
        // Cập nhật pending cache
        pendingPositionCache.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                           .put(slot, positionData);
    }
    
    /**
     * Cập nhật vị trí của player vào main cache (sau khi broadcast)
     */
    public void updatePosition(String gameId, short slot, float x, float y, long timestamp) {
        PositionData positionData = new PositionData(x, y, timestamp);
        
        // Cập nhật main cache
        positionCache.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                    .put(slot, positionData);
        
        // Cập nhật Redis (cho multi-server)
        String key = POSITION_KEY_PREFIX + gameId + ":" + slot;
        redisUtil.set(key, positionData, java.time.Duration.ofSeconds(POSITION_TTL));
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
        // TODO: Implement loading from Redis if needed
        
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
        // TODO: Implement bulk delete from Redis if needed
    }
    
    /**
     * Kiểm tra xem vị trí có hợp lệ không (chống hack)
     */
    public boolean isValidPosition(String gameId, short slot, float x, float y) {
        PositionData lastPosition = getPlayerPosition(gameId, slot);
        if (lastPosition == null) {
            return true; // Lần đầu di chuyển
        }
        
        // Tính khoảng cách di chuyển
        float distance = (float) Math.sqrt(
            Math.pow(x - lastPosition.getX(), 2) + 
            Math.pow(y - lastPosition.getY(), 2)
        );
        
        // Giới hạn tốc độ di chuyển (ví dụ: 10 đơn vị/tick)
        float maxDistance = 10.0f;
        return distance <= maxDistance;
    }
} 