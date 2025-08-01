package com.server.game.netty.receiveObject.pvp;

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
@ReceiveType(ReceiveMessageType.CHAMPION_ATTACK_CHAMPION_RECEIVE)
@Component
@Deprecated
@SuppressWarnings("unused")
public class ChampionChampionReceive implements TLVDecodable{
    short slot;
    short targetSlot; // The slot of the champion being attacked
    long timestamp;

    // Example decode method, assuming TLVDecodable requires this
    @Override
    public void decode(byte[] value) {
        // this.slot = buffer.getShort();
        // this.targetSlot = buffer.getShort();
        // this.timestamp = buffer.getLong();

        // System.out.println(">>> Server Decoded Slot: " + this.slot + ", Target Slot: " + this.targetSlot + ", Timestamp: " + this.timestamp + " Type: 100");
    }
}
