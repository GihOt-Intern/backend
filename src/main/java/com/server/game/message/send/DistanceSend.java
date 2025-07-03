package com.server.game.message.send;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DistanceSend implements TLVEncodable {
    Double distance;

    @Override
    public short getType() {
        return 0x0002;
    }

    @Override
    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        System.out.println("Encoding distance: " + distance);
        buf.putDouble(distance);
        return buf.array();
    }

}
