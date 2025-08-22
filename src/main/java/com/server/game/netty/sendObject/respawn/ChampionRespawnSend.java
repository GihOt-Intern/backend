package com.server.game.netty.sendObject.respawn;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.model.map.component.Vector2;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
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
public class ChampionRespawnSend implements TLVEncodable {
    String championId;
    Vector2 respawnPosition; // Position where the champion will respawn
    float rotate;
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

            byte[] championIdBytes = championId != null ? championId.getBytes("UTF-8") : new byte[0];
            short championIdLength = (short) (short) championIdBytes.length;
            dos.writeShort(championIdLength);
            if (championIdLength > 0) {
                dos.write(championIdBytes);
            }
            dos.writeFloat(respawnPosition.x());
            dos.writeFloat(respawnPosition.y());
            dos.writeFloat(rotate);
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
