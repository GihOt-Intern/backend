package com.server.game.netty.tlv.interf4ce;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;

public interface TLVEncodable { 
    public SendMessageType getType();
    byte[] encode(); // only return the [value] part of the TLV message
    SendTarget getSendTarget(Channel channel);
}
