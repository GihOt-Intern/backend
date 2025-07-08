package com.server.game.netty.pipelineComponent.outboundSendMessage;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OutboundSendMessage {
    ByteBuf byteBuf;
    SendTargetInterface sendTarget;

    public void send() {
        if (sendTarget != null) {
            sendTarget.send(byteBuf);
        } else {
            System.out.println(">>> Error: SendTarget is null, cannot send message.");
        }
    }
}