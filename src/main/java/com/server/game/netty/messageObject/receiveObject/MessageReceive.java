package com.server.game.netty.messageObject.receiveObject;

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
@ReceiveType(ReceiveMessageType.MESSAGE_RECEIVE)
@Component
public class MessageReceive implements TLVDecodable {
    String message;

    @Override
    public void decode(ByteBuffer buffer) { // buffer only contains the [value] part of the TLV message
        int messageByteLength = buffer.getInt();
        byte[] messageBytes = new byte[messageByteLength];
        buffer.get(messageBytes);
        this.message = Util.bytesToString(messageBytes);
        System.out.println(">>> Server Decoded Message: " + this.message);
    }
}
