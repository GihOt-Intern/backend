package com.server.game.tlv.codec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.server.game.tlv.serializationable.TLVEncodable;

public class TLVEncoder {

    public static byte[] object2Byte(TLVEncodable sendObj) {
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