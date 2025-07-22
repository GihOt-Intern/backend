package com.server.game.netty.receiveMessageHandler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.receiveObject.PositionReceive;
import com.server.game.service.MoveService;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PositionHandler {
    private final MoveService moveService;
    
    @MessageMapping(PositionReceive.class)
    public void handlePosition(PositionReceive receiveObject, Channel channel) {

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

        moveService.setMoveTarget(
            gameId,
            slot,
            receiveObject.getPosition()
        );

        System.out.println(">>> Position updated for gameId: " + gameId + ", slot: " + slot +
            ", X: " + receiveObject.getPosition().x() + ", Y: " + receiveObject.getPosition().y() + ", timestamp: " + timestamp);
    }
} 