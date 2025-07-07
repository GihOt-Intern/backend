package com.server.game.netty.messageObject.receiveObject;

import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.codecableInterface.TLVDecodable;
import com.server.game.netty.tlv.typeDefine.ClientMessageType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ClientMessageType.DISTANCE_RECEIVE)
@Component
public class DistanceReceive implements TLVDecodable {
    Double x1;
    Double y1;
    Double x2;
    Double y2;

    @Override
    public void decode(ByteBuffer buffer) { // buffer only contains the [value] part of the TLV message
        x1 = buffer.getDouble();
        y1 = buffer.getDouble();
        x2 = buffer.getDouble();
        y2 = buffer.getDouble();
    }
}
