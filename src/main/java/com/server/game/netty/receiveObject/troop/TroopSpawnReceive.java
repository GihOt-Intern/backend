package com.server.game.netty.receiveObject.troop;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.messageEnum.ReceiveMessageType;

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
    public void decode(byte[] value) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(value);
            DataInputStream dis = new DataInputStream(bais);
            this.troopId = dis.readShort();
            this.ownerSlot = dis.readShort();
            this.timestamp = dis.readLong();
        } catch (Exception e) {
            throw new  RuntimeException("Cannot decode " + this.getClass().getSimpleName(), e);
        }
    }
}
