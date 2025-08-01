package com.server.game.netty.receiveObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.messageEnum.ReceiveMessageType;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.DISTANCE_RECEIVE) // Custom annotation to define the type of this message
@Component // Register this class as a Spring component to be scanned in ReceiveTypeScanner when the application starts
public class DistanceReceive implements TLVDecodable {
    float x1;
    float y1;
    float x2;
    float y2;

    @Override
    public void decode(byte[] value) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(value);
            DataInputStream dis = new DataInputStream(bais);

            this.x1 = dis.readFloat();
            this.y1 = dis.readFloat();
            this.x2 = dis.readFloat();
            this.y2 = dis.readFloat();

        } catch (Exception e) {
            throw new  RuntimeException("Cannot decode " + this.getClass().getSimpleName(), e);
        }
    }
}
