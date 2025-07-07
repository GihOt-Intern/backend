package com.server.game.netty.tlv.codec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.server.game.netty.tlv.codecableInterface.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.ServerMessageType;

public class TLVEncoder {

    public static byte[] object2Byte(TLVEncodable sendObj) {
        byte[] valueBytes = sendObj.encode();
        ServerMessageType type = sendObj.getType();
        int length = valueBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(2 + 4 + length).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(type.getType());
        buffer.putInt(length);
        buffer.put(valueBytes);

        return buffer.array();
    }
}