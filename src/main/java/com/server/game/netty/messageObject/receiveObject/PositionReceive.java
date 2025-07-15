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
@ReceiveType(ClientMessageType.POSITION_UPDATE_RECEIVE)
@Component
public class PositionReceive implements TLVDecodable {
    short slot;
    float x;
    float y;
    long timestamp;

    @Override
    public void decode(ByteBuffer buffer) {
        slot = buffer.getShort();
        x = buffer.getFloat();
        y = buffer.getFloat();
        timestamp = buffer.getLong();
    }
} 