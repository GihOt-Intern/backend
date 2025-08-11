package com.server.game.netty.sendObject.entity;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EntitiesRemovedSend implements TLVEncodable {
    List<String> entityIds;
    long timestamp;

    @Override
    public SendMessageType getType() {
        return SendMessageType.ENTITIES_REMOVED;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeShort(entityIds.size());

            for(String id : entityIds) {
                byte[] idBytes = id.getBytes();
                dos.writeShort(idBytes.length);
                dos.write(idBytes);
            }

            dos.writeLong(timestamp);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error encoding EntitiesRemovedSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}