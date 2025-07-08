package com.server.game.netty.tlv.codecableInterface;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTargetInterface;
import com.server.game.netty.tlv.typeDefine.ServerMessageType;

import io.netty.channel.Channel;

public interface TLVEncodable { 
    public ServerMessageType getType();
    byte[] encode(); // only return the [value] part of the TLV message
    SendTargetInterface getSendTarget(Channel channel);
}
