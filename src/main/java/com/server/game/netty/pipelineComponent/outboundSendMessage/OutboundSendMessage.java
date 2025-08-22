package com.server.game.netty.pipelineComponent.outboundSendMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Getter
@Slf4j
public class OutboundSendMessage {
    ByteBuf byteBuf;
    SendTarget sendTarget;

    public ChannelFuture send() {
        if (sendTarget != null) {
            return sendTarget.send(byteBuf);
        } else {
            log.error(">>> Error: SendTarget is null, cannot send message.");
            return new DefaultChannelPromise(null)
                .setFailure(new IllegalStateException("SendTarget is null"));
        }
    }
}