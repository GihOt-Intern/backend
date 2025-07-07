package com.server.game.netty.tlv.codecableInterface;

import com.server.game.netty.tlv.typeDefine.ServerMessageType;

public interface TLVEncodable { 
    public ServerMessageType getType();
    byte[] encode(); // only return the [value] part of the TLV message
}
