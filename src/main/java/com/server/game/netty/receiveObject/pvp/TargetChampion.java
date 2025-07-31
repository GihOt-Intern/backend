package com.server.game.netty.receiveObject.pvp;
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

@Deprecated
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.TARGET_ATTACK_CHAMPION_RECEIVE)
@Component
public class TargetChampion implements TLVDecodable {
    String attackerId;
    short slot;
    long timestamp;

    @Override
    public void decode(ByteBuffer buffer) {
        short attackerIdLength = buffer.getShort();
        byte[] attackerIdBytes = new byte[attackerIdLength];
        buffer.get(attackerIdBytes);
        this.attackerId = Util.bytesToString(attackerIdBytes);
        
        this.slot = buffer.getShort();
        this.timestamp = buffer.getLong();

        System.out.println(">>> Server Decoded Attacker ID: " + this.attackerId + ", Slot: " + this.slot + ", Timestamp: " + this.timestamp + " Type: 102");
    }
}
