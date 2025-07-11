package com.server.game.netty.messageObject.sendObject;

import java.nio.ByteBuffer;

import io.netty.channel.Channel;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
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
    short slot;
    Integer championId;

    @Override
    public ServerMessageType getType() {
        return ServerMessageType.CHOOSE_CHAMPION_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        // byte[] userIdBytes = Util.stringToBytes(this.userId);
        // int userIdByteLength = userIdBytes.length;

        // ByteBuffer buf = Util.allocateByteBuffer(Util.INT_SIZE + userIdByteLength + Util.INT_SIZE);

        // buf.putInt(userIdByteLength);
        // buf.put(userIdBytes);
        // buf.putInt(this.championId);

        // Util.printHex(buf, true);
        
        ByteBuffer buf = Util.allocateByteBuffer(Util.SHORT_SIZE + Util.INT_SIZE);
        buf.putShort(slot);
        buf.putInt(championId);
        return buf.array();
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
