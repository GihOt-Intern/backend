package com.server.game.netty.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.netty.channel.Channel;

import com.server.game.model.game.Entity;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
import com.server.game.service.move.MoveService.PositionData;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PositionSend implements TLVEncodable {
    List<EntityPositionData> entities;
    long timestamp;

    public PositionSend(Map<Entity, PositionData> gameStatePendingPositions, long timestamp) {
        this.entities = gameStatePendingPositions.entrySet().stream()
            .map(entry -> {
                Entity entity = entry.getKey();
                PositionData positionData = entry.getValue();
                return new EntityPositionData(entity, positionData);
            })
            .collect(Collectors.toList());
        this.timestamp = timestamp;
    }

    @Override
    public SendMessageType getType() {
        return SendMessageType.POSITION_UPDATE_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write number of players
            dos.writeShort(entities.size());

            // Write each entity's data
            for (EntityPositionData entity : entities) {
                dos.write(entity.encode());
            }
            
            // Write timestamp
            dos.writeLong(timestamp);

            // ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
            // Util.printHex(buffer, true);
            
            
            
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode PositionSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
    
    // Inner class để lưu trữ dữ liệu vị trí player
    @Data
    @AllArgsConstructor
    public static class EntityPositionData {
        String stringId;
        Vector2 position;
        float speed;

        public EntityPositionData(Entity entity, PositionData positionData) {
            this.stringId = entity.getStringId();
            this.position = positionData.getPosition();
            this.speed = positionData.getSpeed();
        }


        public byte[] encode() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeUTF(stringId); // this method already adds first 2 bytes for byte length
                dos.writeFloat(position.x());
                dos.writeFloat(position.y());
                dos.writeFloat(speed);

                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Cannot encode PlayerPositionData", e);
            }
        }
    }
} 