package com.server.game.netty.messageObject.sendObject.respawn;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.SendMessageType;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChampionDeathSend implements TLVEncodable {
    short championSlot;

    @Override
    public SendMessageType getType() {
        return SendMessageType.CHAMPION_DEATH_SEND;
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeShort(championSlot);
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error encoding ChampionDeathSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
