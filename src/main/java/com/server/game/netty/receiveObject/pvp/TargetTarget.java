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
@ReceiveType(ReceiveMessageType.TARGET_ATTACK_TARGET_RECEIVE)
@Component
public class TargetTarget implements TLVDecodable {
    String targetId;
    short slot;
    long timestamp;

    @Override
    public void decode(ByteBuffer buffer) {
        short targetIdLength = buffer.getShort();
        byte[] targetIdBytes = new byte[targetIdLength];
        buffer.get(targetIdBytes);
        this.targetId = Util.bytesToString(targetIdBytes);
        this.slot = buffer.getShort();
        this.timestamp = buffer.getLong();

        System.out.println(">>> Server Decoded Target ID: " + this.targetId + ", Slot: " + this.slot + ", Timestamp: " + this.timestamp + " Type: 103");
    }
}
