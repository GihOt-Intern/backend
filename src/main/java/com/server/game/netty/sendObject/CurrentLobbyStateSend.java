package com.server.game.netty.messageObject.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import io.netty.channel.Channel;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.SendMessageType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurrentLobbyStateSend implements TLVEncodable {
    List<PlayerLobbyStateData> players;



    @Override
    public SendMessageType getType() {
        return SendMessageType.CURRENT_LOBBY_STATE_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write number of players
            dos.writeShort(players.size());
            
            // Write each player's data
            for (PlayerLobbyStateData player : players) {
                dos.write(player.encode());
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode CurrentLobbyStateSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
    
    // Inner class để lưu trạng thái của từng người chơi trong phòng chờ
    @Data
    @AllArgsConstructor
    public static class PlayerLobbyStateData {
        short slot;
        short choseChampionId; // -1 if not chosen
        boolean isReady;


        public byte[] encode() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeShort(slot);
                dos.writeShort(choseChampionId);
                dos.writeBoolean(isReady);

                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Cannot encode PlayerLobbyStateData", e);
            }
        }
    }
} 