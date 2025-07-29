package com.server.game.netty.receiveObject.pvp;

import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.map.component.Vector2;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.messageEnum.ReceiveMessageType;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.CHAMPION_SKILL_CAST_RECEIVE)
@Component
public class SkillCastReceive implements TLVDecodable {
    short slot;
    Vector2 targetPosition;
    long timestamp;

    @Override
    public void decode(ByteBuffer buffer) {
        this.slot = buffer.getShort();

        float x = buffer.getFloat();
        float y = buffer.getFloat();
        this.targetPosition = new Vector2(x, y);

        this.timestamp = buffer.getLong();
    }
}
