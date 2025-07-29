package com.server.game.netty.sendObject.respawn;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChampionRespawnTimeSend implements TLVEncodable{
    short respawnTime;

    @Override
    public SendMessageType getType() {
        return SendMessageType.CHAMPION_RESPAWN_TIME_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeShort(respawnTime);

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error encoding ChampionRespawnTimeSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
