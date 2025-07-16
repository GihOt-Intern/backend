package com.server.game.netty.messageObject.sendObject;

import java.nio.ByteBuffer;

import io.netty.channel.Channel;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.SendMessageType;
import com.server.game.util.ChampionEnum;
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
    ChampionEnum championId;

    @Override
    public SendMessageType getType() {
        return SendMessageType.CHOOSE_CHAMPION_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        ByteBuffer buf = Util.allocateByteBuffer(Util.SHORT_SIZE + Util.SHORT_SIZE);
        buf.putShort(slot);
        buf.putShort(championId.getChampionId());
        return buf.array();
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
