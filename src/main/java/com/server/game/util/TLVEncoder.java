package com.server.game.util;

import com.server.game.message.send.TLVEncodable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TLVEncoder {

    public static byte[] encode(TLVEncodable sendObj) {
        byte[] valueBytes = sendObj.encode();
        short type = sendObj.getType();
        int length = valueBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(2 + 4 + length).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(type);
        buffer.putInt(length);
        buffer.put(valueBytes);

        return buffer.array();
    }
}