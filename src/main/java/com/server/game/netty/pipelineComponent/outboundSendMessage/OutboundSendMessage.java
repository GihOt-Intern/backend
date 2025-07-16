package com.server.game.netty.pipelineComponent.outboundSendMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OutboundSendMessage {
    ByteBuf byteBuf;
    SendTarget sendTarget;

    public ChannelFuture send() {
        if (sendTarget != null) {
            return sendTarget.send(byteBuf);
        } else {
            System.out.println(">>> Error: SendTarget is null, cannot send message.");
            return new DefaultChannelPromise(null).setFailure(new IllegalStateException("SendTarget is null"));
        }
    }
}