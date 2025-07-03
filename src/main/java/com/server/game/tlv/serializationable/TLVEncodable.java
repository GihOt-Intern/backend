package com.server.game.tlv.serializationable;


public interface TLVEncodable {
    public short getType();
    byte[] encode();
}
