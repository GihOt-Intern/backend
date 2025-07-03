package com.server.game.message.TLVInterface;


public interface TLVDecodable {
    // short getType();
    void decode(byte[] value);
}
