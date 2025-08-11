package com.server.game.netty.sendObject.entity;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
public class EntityDeathSend implements TLVEncodable {
    String entityId;

    @Override
    public SendMessageType getType() {
        return SendMessageType.ENTITY_DEATH_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            byte[] entityIdBytes = entityId != null ? entityId.getBytes("UTF-8") : new byte[0];
            short entityIdLength = (short) (short) entityIdBytes.length;
            dos.writeShort(entityIdLength);
            if (entityIdLength > 0) {
                dos.write(entityIdBytes);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error encoding EntityDeathSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
