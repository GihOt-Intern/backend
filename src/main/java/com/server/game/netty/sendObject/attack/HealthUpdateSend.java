package com.server.game.netty.sendObject.attack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthUpdateSend implements TLVEncodable {
    String targetId;
    int currentHealth;
    int maxHealth;
    int damage;
    long timestamp;

    // Constructor for champion health update
    public HealthUpdateSend(String targetId, int currentHealth, int maxHealth, int damage, long timestamp) {
        this.targetId = targetId;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.timestamp = timestamp;
    }


    @Override
    public SendMessageType getType() {
        return SendMessageType.HEALTH_UPDATE_SEND;
    }

    @Override
    public byte[] encode() {


        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeUTF(targetId);
            dos.writeInt(currentHealth);
            dos.writeInt(maxHealth);
            dos.writeInt(damage);
            dos.writeLong(timestamp);


            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode HealthUpdateSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
