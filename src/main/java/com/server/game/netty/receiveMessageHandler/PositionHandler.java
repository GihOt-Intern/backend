package com.server.game.netty.receiveMessageHandler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.receiveObject.PositionReceive;
import com.server.game.service.PositionService;
import com.server.game.service.PositionBroadcastService;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PositionHandler {
    
    private final PositionService positionService;
    private final PositionBroadcastService positionBroadcastService;
    
    @MessageMapping(PositionReceive.class)
    public void handlePosition(PositionReceive receiveObject, Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        short slot = receiveObject.getSlot();
        
        if (gameId == null) {
            System.out.println(">>> Invalid gameId for position update");
            return;
        }
        
        // Kiểm tra slot có hợp lệ không (chống hack)
        short expectedSlot = ChannelManager.getSlotByChannel(channel);
        if (slot != expectedSlot) {
            System.out.println(">>> Slot mismatch: received " + slot + ", expected " + expectedSlot);
            return;
        }
        
        // Kiểm tra vị trí có hợp lệ không (chống hack)
        if (!positionService.isValidPosition(gameId, slot, receiveObject.getX(), receiveObject.getY())) {
            System.out.println(">>> Invalid position detected for slot: " + slot);
            return;
        }
        
        // Chỉ lưu vị trí vào pending cache, không broadcast ngay
        positionService.updatePendingPosition(gameId, slot, receiveObject.getX(), receiveObject.getY(), receiveObject.getTimestamp());
        
        // Đăng ký game với broadcast service nếu chưa đăng ký
        positionBroadcastService.registerGame(gameId);
        
        System.out.println(">>> Position stored in pending cache - Game: " + gameId + 
                          ", Slot: " + slot + 
                          ", Position: (" + receiveObject.getX() + ", " + receiveObject.getY() + ")");
    }
    
    // Lấy vị trí của tất cả player trong game
    public Map<Short, PositionData> getGamePositions(String gameId) {
        return positionService.getGamePositions(gameId);
    }
    
    // Xóa dữ liệu vị trí khi game kết thúc
    public void clearGamePositions(String gameId) {
        positionService.clearGamePositions(gameId);
        positionBroadcastService.unregisterGame(gameId);
    }
    
    // Inner class để lưu trữ dữ liệu vị trí
    public static class PositionData {
        private final float x, y;
        private final long timestamp;
        
        public PositionData(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }
        
        public float getX() { return x; }
        public float getY() { return y; }
        public long getTimestamp() { return timestamp; }
    }
} 