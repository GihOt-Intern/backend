package com.server.game.tlv.serializationable;


public interface TLVDecodable {
    // short getType();
    void decode(byte[] value);
}
