package com.server.game.netty.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestGameStartResponseSend implements TLVEncodable {
    List<PlayerInfo> players;

    @Override
    public SendMessageType getType() {
        return SendMessageType.TEST_GAME_START_RESPONSE;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write number of players
            dos.writeShort(players.size());

            // Write each player's data
            for (PlayerInfo player : players) {
                dos.writeShort(player.getSlot());
                dos.writeShort(player.getChampionId());
            } 
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode TestGameStartResponseSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerInfo {
        short slot;
        short championId;

        // Additional fields can be added here if needed
    }
}
