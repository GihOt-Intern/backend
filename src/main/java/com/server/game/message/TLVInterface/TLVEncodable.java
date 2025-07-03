package com.server.game.message.TLVInterface;


public interface TLVEncodable {
    public short getType();
    byte[] encode();
}
