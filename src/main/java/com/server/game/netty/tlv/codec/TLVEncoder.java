package com.server.game.netty.tlv.codec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
import com.server.game.util.Util;

public class TLVEncoder {

    public static byte[] object2Bytes(TLVEncodable sendObj) { // return a full TLV message in byte array format
        byte[] valueBytes = sendObj.encode();
        SendMessageType type = sendObj.getType();
        int length = valueBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(Util.SHORT_SIZE + Util.INT_SIZE + length).order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(type.getType());
        buffer.putInt(length);
        buffer.put(valueBytes);

        byte[] result = buffer.array();
        
        return result;
    }
}