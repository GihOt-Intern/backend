package com.server.game.message.receive;

import java.nio.ByteBuffer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DistanceReceive implements TLVDecodable {
    Double x1;
    Double y1;
    Double x2;
    Double y2;

    @Override
    public void decode(byte[] value) {
        ByteBuffer buffer = ByteBuffer.wrap(value);
        x1 = buffer.getDouble();
        y1 = buffer.getDouble();
        x2 = buffer.getDouble();
        y2 = buffer.getDouble();
    }
}
