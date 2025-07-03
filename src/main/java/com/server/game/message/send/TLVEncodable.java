package com.server.game.message.send;

import java.nio.ByteBuffer;

public interface TLVEncodable {
    byte[] encode(short type);
}
