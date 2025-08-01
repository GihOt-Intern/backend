package com.server.game.netty.messageHandler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.model.game.context.AttackContext;
import com.server.game.netty.receiveObject.attack.AttackReceive;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class AttackMessageHandler {
    
    // @MessageMapping(AttackReceive.class)
    // public void handleAttackMessage(AttackReceive receiveObject) {
    //     try {
    //         // Extract context from the received object
    //         AttackContext ctx = 
    //     } catch (Exception e) {
    //         log.error("Error handling attack message: {}", e.getMessage(), e);
    //     }
    // }
} 