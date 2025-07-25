package com.server.game.netty.messageObject.receiveObject.troop;

import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.typeDefine.ReceiveMessageType;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.TROOP_SPAWN_RECEIVE)
@Component
public class TroopSpawnReceive implements TLVDecodable {
    short troopId;
    short ownerSlot;
    long timestamp;

    @Override
    public void decode(ByteBuffer buffer) {
        this.troopId = buffer.getShort();
        this.ownerSlot = buffer.getShort();
        this.timestamp = buffer.getLong();
    }
}
