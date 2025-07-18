package com.server.game.netty.messageObject.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import io.netty.channel.Channel;

import com.server.game.map.component.Vector2;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.SendMessageType;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PositionSend implements TLVEncodable {
    List<PlayerPositionData> players;
    long timestamp;

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
            dos.writeShort(players.size());
            
            // Write each player's data
            for (PlayerPositionData player : players) {
                dos.write(player.encode());
            }
            
            // Write timestamp
            dos.writeLong(timestamp);



            System.out.println("PositionSend byte:");
            // Util.printHex(new ByteBuffer(baos.toByteArray()), true);
            ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
            Util.printHex(buffer, true);
            
            
            
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
    public static class PlayerPositionData {
        short slot;
        Vector2 position;


        public byte[] encode() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeShort(slot);
                dos.writeFloat(position.x());
                dos.writeFloat(position.y());

                System.out.println("Slot: " + slot + ", X: " + position.x() + ", Y: " + position.y());

                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Cannot encode PlayerPositionData", e);
            }
        }
    }
} 