package com.server.game.netty.messageObject.receiveObject;

import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.codecableInterface.TLVDecodable;
import com.server.game.netty.tlv.typeDefine.ClientMessageType;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ClientMessageType.AUTHENTICATION_RECEIVE)
@Component
public class AuthenticationReceive implements TLVDecodable {
    String token;
    String gameId;

    @Override
    public void decode(ByteBuffer buffer) { // buffer only contains the [value] part of the TLV message
        int tokenByteLength = buffer.getInt();
        byte[] tokenBytes = new byte[tokenByteLength];
        buffer.get(tokenBytes);
        this.token = Util.bytesToString(tokenBytes);

        int gameIdByteLength = buffer.getInt();
        byte[] gameIdBytes = new byte[gameIdByteLength];
        buffer.get(gameIdBytes);
        this.gameId = Util.bytesToString(gameIdBytes);

        System.out.println(">>> Server Decoded Token: " + this.token + ", Game ID: " + this.gameId);
    }
}
