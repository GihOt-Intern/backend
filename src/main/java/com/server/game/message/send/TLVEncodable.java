package com.server.game.message.send;


public interface TLVEncodable {
    public short getType();
    byte[] encode();
}
