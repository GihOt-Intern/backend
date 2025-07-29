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
@ReceiveType(ReceiveMessageType.LOBBY_LOADED_RECEIVE)
@Component
public class LobbyLoadedReceive implements TLVDecodable {
    // There are no fields 

    @Override
    public void decode(ByteBuffer buffer) { // buffer only contains the [value] part of the TLV message
        
    }
}
