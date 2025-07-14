package com.server.game.netty.messageObject.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import io.netty.channel.Channel;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.codecableInterface.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.ServerMessageType;

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
    public ServerMessageType getType() {
        return ServerMessageType.POSITION_UPDATE_SEND;
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
                dos.writeShort(player.getSlot());
                dos.writeFloat(player.getX());
                dos.writeFloat(player.getY());
            }
            
            // Write timestamp
            dos.writeLong(timestamp);

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
        float x;
        float y;
    }
} 