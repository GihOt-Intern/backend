package com.server.game.netty.messageObject.sendObject.respawn;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.map.component.Vector2;
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
public class ChampionRespawnSend implements TLVEncodable {
    short championSlot;
    Vector2 respawnPosition; // Position where the champion will respawn
    int maxHealth;
    
    @Override
    public SendMessageType getType() {
        return SendMessageType.CHAMPION_RESPAWN_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeShort(championSlot);
            dos.writeFloat(respawnPosition.x());
            dos.writeFloat(respawnPosition.y());
            dos.writeInt(maxHealth);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error encoding ChampionRespawnSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
