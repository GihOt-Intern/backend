package com.server.game.netty.tlv.interf4ce;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.tlv.typeDefine.ServerMessageType;

import io.netty.channel.Channel;

public interface TLVEncodable { 
    public ServerMessageType getType();
    byte[] encode(); // only return the [value] part of the TLV message
    SendTarget getSendTarget(Channel channel);
}
