package com.server.game.netty.pipelineComponent.outboundSendMessage;

import io.netty.buffer.ByteBuf;

public interface SendTargetInterface {
    public abstract void send(ByteBuf payload);
}   