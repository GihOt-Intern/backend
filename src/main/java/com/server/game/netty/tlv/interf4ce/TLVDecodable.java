package com.server.game.netty.tlv.interf4ce;


public interface TLVDecodable {
    void decode(byte[] value); // buffer only contains the [value] part of the TLV message
}
