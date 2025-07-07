package com.server.game.netty.handler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.messageObject.receiveObject.MessageReceive;

// import com.server.game.ws.messageMapping.MessageMapping;

@Component
public class ChatHandler {

    @MessageMapping(MessageReceive.class)
    public void handleClientMessage(MessageReceive msg) {
        System.out.println("Received message from client: " + msg.getMessage());    
    }



}
