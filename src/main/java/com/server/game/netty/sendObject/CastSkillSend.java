package com.server.game.netty.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

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
public class CastSkillSend implements TLVEncodable {
    String casterId;
    Vector2 targetPosition; // Caster's mouse position
    float skillLength;
    long timestamp;

    @Override
    public SendMessageType getType() {
        return SendMessageType.CAST_SKILL_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeUTF(casterId);
            dos.writeFloat(targetPosition.x());
            dos.writeFloat(targetPosition.y());
            dos.writeFloat(skillLength);
            dos.writeLong(timestamp);

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Cannot encode " + this.getClass().getSimpleName(), e);
        }
    }


    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
