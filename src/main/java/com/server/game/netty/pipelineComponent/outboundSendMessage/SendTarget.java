package com.server.game.netty.pipelineComponent.outboundSendMessage;

import io.netty.buffer.ByteBuf;

public interface SendTarget {
    public void send(ByteBuf message);
}   