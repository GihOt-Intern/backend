package com.server.game.message.receive;


public interface TLVDecodable {
    void decode(byte[] value);
}
