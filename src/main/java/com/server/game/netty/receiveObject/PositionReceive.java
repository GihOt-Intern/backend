package com.server.game.netty.receiveObject;

import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.messageEnum.ReceiveMessageType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.POSITION_UPDATE_RECEIVE)
@Component
public class PositionReceive implements TLVDecodable {
    short slot;
    Vector2 position;
    long timestamp;

    @Override
    public void decode(ByteBuffer buffer) {
        slot = buffer.getShort();
        position = new Vector2(buffer.getFloat(), buffer.getFloat());
        timestamp = buffer.getLong();
    }
} 