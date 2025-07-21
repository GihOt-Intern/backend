package com.server.game.netty.receiveMessageHandler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.receiveObject.PositionReceive;
import com.server.game.service.AttackTargetingService;
import com.server.game.service.MoveService;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PositionHandler {
    private final MoveService moveService;
    private final AttackTargetingService attackTargetingService;
    
    @MessageMapping(PositionReceive.class)
    public void handlePosition(PositionReceive receiveObject, ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();

        String gameId = ChannelManager.getGameIdByChannel(channel);
        Short slot = receiveObject.getSlot();
        long timestamp = receiveObject.getTimestamp();
        
        if (gameId == null || slot == null) {
            System.out.println(">>> Invalid gameId for position update");
            return;
        }
        
        // Kiểm tra slot có hợp lệ không (chống hack)
        short expectedSlot = ChannelManager.getSlotByChannel(channel);
        if (slot != expectedSlot) {
            System.out.println(">>> Slot mismatch: received " + slot + ", expected " + expectedSlot);
            return;
        }

        // Clear attack target when player manually moves
        attackTargetingService.clearAttackTarget(gameId, slot);

        moveService.setMoveTarget(
            gameId,
            slot,
            receiveObject.getPosition(),
            5.0f
        );

        System.out.println(">>> Position updated for gameId: " + gameId + ", slot: " + slot +
            ", X: " + receiveObject.getPosition().x() + ", Y: " + receiveObject.getPosition().y() + ", timestamp: " + timestamp);
    }
    
    // Inner class để lưu trữ dữ liệu vị trí
    @Data
    public static class PositionData {
        private final Vector2 position;
        private final long timestamp;
    }
} 