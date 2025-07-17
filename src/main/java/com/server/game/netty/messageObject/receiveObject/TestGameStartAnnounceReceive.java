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
@ReceiveType(ReceiveMessageType.TEST_GAME_START_ANNOUNCE)
@Component
public class TestGameStartAnnounceReceive implements TLVDecodable {
    String gameId;

    @Override
    public void decode(ByteBuffer buf) {
        short gameIdLength = buf.getShort();
        byte[] gameIdBytes = new byte[gameIdLength];
        buf.get(gameIdBytes);
        this.gameId = Util.bytesToString(gameIdBytes);
    }
}
