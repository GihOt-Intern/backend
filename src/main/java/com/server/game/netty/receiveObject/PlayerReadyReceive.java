package com.server.game.netty.receiveObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.PLAYER_READY_RECEIVE) // Custom annotation to define the type of this message
@Component // Register this class as a Spring component to be scanned in ReceiveTypeScanner when the application starts
public class PlayerReadyReceive implements TLVDecodable {
    // No data, just a signal message

    @Override
    public void decode(byte[] value) {
    }
}
