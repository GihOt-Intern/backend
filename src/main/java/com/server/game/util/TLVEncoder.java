package com.server.game.util;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.server.game.message.send.DistanceSend;

public class TLVEncoder {

    public static byte[] encode(short type, Object dto) {
        byte[] value;

        
        // TODO: try refactor
        
        if (dto instanceof DistanceSend) {
            value = encodeDistanceResponse((DistanceSend) dto);
        } //  else if (dto instanceof AnotherSend) { }
        else {
            throw new IllegalArgumentException("Unknown DTO class: " + dto.getClass().getName());
        }

        int length = value.length;
        ByteBuffer buf = ByteBuffer.allocate(2 + 4 + length).order(ByteOrder.BIG_ENDIAN);
        buf.putShort(type);
        buf.putInt(length);
        buf.put(value);

        return buf.array();
    }


    private static byte[] encodeDistanceResponse(DistanceSend dto) {
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        System.out.println("Encoding distance: " + dto.getDistance());
        buf.putDouble(dto.getDistance());
        return buf.array();
    }
}