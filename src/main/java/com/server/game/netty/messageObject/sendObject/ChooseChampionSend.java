package com.server.game.netty.messageObject.sendObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import com.server.game.netty.tlv.codecableInterface.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.ServerMessageType;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChooseChampionSend implements TLVEncodable {
    String userId;
    Integer championId;

    @Override
    public ServerMessageType getType() {
        return ServerMessageType.CHOOSE_CHAMPION_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        int userIdLength = this.userId.length();
        
        ByteBuffer buf = ByteBuffer
            .allocate(Util.INT_SIZE + Util.STRING_BYTE_SIZE(userIdLength) + Util.INT_SIZE)
            .order(ByteOrder.BIG_ENDIAN);
        byte[] userIdBytes = this.userId.getBytes(Charset.forName("UTF-32BE"));
        // print userIdBytes in hex
        Util.printHex(ByteBuffer.wrap(userIdBytes));

        buf.putInt(userIdLength);
        buf.put(userIdBytes);
        buf.putInt(this.championId);

        Util.printHex(buf);
        return buf.array();
    }
}
