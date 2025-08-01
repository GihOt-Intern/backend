package com.server.game.netty.receiveObject.attack;

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
@ReceiveType(ReceiveMessageType.ATTACK_RECEIVE)
@Component
public class AttackReceive implements TLVDecodable {
    String attackerId;
    String targetId;

    @Override
    public void decode(byte[] value) { // buffer only contains the [value] part of the TLV message
        
    }
}
