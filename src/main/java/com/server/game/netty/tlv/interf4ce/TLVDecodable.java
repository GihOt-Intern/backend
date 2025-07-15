package com.server.game.netty.tlv.interf4ce;

import java.nio.ByteBuffer;


public interface TLVDecodable {
    void decode(ByteBuffer buffer); // buffer only contains the [value] part of the TLV message
}
