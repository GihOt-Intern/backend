package com.server.game.netty.messageObject.receiveObject.pvp;

import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.typeDefine.ReceiveMessageType;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.util.Util;

import lombok.AccessLevel;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.CHAMPION_ATTACK_TARGET_RECEIVE)
@Component
public class ChampionTarget implements TLVDecodable{
    String targetId;
    long timestamp;

    // Example decode method, assuming TLVDecodable requires this
    @Override
    public void decode(ByteBuffer buffer) {
        short targetIdLength = buffer.getShort();
        byte[] targetIdBytes = new byte[targetIdLength];
        buffer.get(targetIdBytes);
        this.targetId = Util.bytesToString(targetIdBytes);
        this.timestamp = buffer.getLong();

        System.out.println(">>> Server Decoded Target ID: " + this.targetId + ", Timestamp: " + this.timestamp + " Type: 101");
    }
}
