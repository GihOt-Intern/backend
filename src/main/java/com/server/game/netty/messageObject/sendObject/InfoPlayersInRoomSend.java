package com.server.game.netty.messageObject.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import io.netty.channel.Channel;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.codecableInterface.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.ServerMessageType;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InfoPlayersInRoomSend implements TLVEncodable {
    Map<Short, String> players;

    @Override
    public ServerMessageType getType() {
        return ServerMessageType.INFO_PLAYERS_IN_ROOM_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeShort(players.size());
            for (Map.Entry<Short, String> entry : players.entrySet()) {
                short slot = entry.getKey();
                String username = entry.getValue();
                byte[] usernameBytes = Util.stringToBytes(username);
                int usernameByteLength = usernameBytes.length;

                dos.writeInt(usernameByteLength); // write the length of username
                dos.write(usernameBytes); // write the username bytes
                dos.writeShort(slot); // write the slot number
            }

            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Cannot encoding InfoPlayersInRoomSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
