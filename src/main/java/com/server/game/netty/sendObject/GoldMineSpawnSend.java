package com.server.game.netty.sendObject;

import com.server.game.model.map.component.Vector2;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

@AllArgsConstructor
public class GoldMineSpawnSend implements TLVEncodable {
    String stringId;
    Vector2 position;
    boolean isSmall;
    int initHP;
    

    @Override
    public SendMessageType getType() {
        return SendMessageType.GOLD_MINE_SPAWN_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeUTF(stringId);
            dos.writeFloat(position.x());
            dos.writeFloat(position.y());
            dos.writeBoolean(isSmall);
            dos.writeInt(initHP);

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Cannot encode " + this.getClass().getSimpleName(), e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }

    @Override
    public String toString() {
        return "GoldMineSpawnSend{" +
                "position=" + position +
                ", isSmall=" + isSmall +
                '}';
    }
}