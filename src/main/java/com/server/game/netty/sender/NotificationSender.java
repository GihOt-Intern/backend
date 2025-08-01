package com.server.game.netty.sender;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.receiveObject.attack.AttackReceive;
import com.server.game.netty.sendObject.PongSend;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NotificationSender {

    @MessageMapping(AttackReceive.class)
    public PongSend sendPongMessage(AttackReceive receiveObject) {
        return new PongSend();
    }

} 