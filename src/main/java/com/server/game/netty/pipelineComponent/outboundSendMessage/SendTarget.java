package com.server.game.netty.pipelineComponent.outboundSendMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;

public interface SendTarget {
    public ChannelFuture send(ByteBuf message);
}   