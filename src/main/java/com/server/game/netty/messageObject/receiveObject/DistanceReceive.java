package com.server.game.netty.messageObject.receiveObject;

import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.typeDefine.ClientMessageType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ClientMessageType.DISTANCE_RECEIVE) // Custom annotation to define the type of this message
@Component // Register this class as a Spring component to be scanned in ReceiveTypeScanner when the application starts
public class DistanceReceive implements TLVDecodable {
    float x1;
    float y1;
    float x2;
    float y2;

    @Override // must override this method of TLVDecodable interface
    public void decode(ByteBuffer buffer) { // buffer only contains the [value] part of the TLV message
        x1 = buffer.getFloat();
        y1 = buffer.getFloat();
        x2 = buffer.getFloat();
        y2 = buffer.getFloat();
    }
}
