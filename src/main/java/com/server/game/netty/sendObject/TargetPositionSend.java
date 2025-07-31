package com.server.game.netty.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import io.netty.channel.Channel;

import com.server.game.model.map.component.Vector2;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetPositionSend implements TLVEncodable{
    List<TargetPositionData> targets;
    long timestamp;

    @Override
    public SendMessageType getType() {
        return SendMessageType.TARGET_POSITION_UPDATE_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write number of targets
            dos.writeShort(targets.size());
            
            // Write each target's data
            for (TargetPositionData target : targets) {
                dos.write(target.encode());
            }
            
            // Write timestamp
            dos.writeLong(timestamp);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode TargetPositionSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }

    @Data
    @AllArgsConstructor
    public static class TargetPositionData {
        String targetId;
        Vector2 position;

        public byte[] encode() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                byte[] targetIdBytes = Util.stringToBytes(targetId);
                int targetIdLength = targetIdBytes.length;

                // Write target ID length and ID
                dos.writeShort(targetIdLength);
                dos.write(targetIdBytes);
                
                // Write position
                dos.writeFloat(position.x());
                dos.writeFloat(position.y());

                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Cannot encode TargetPositionData", e);
            }
        }
    }
}
