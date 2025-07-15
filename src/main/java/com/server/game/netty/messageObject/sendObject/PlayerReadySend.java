package com.server.game.netty.messageObject.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.netty.channel.Channel;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.ServerMessageType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerReadySend implements TLVEncodable {
    short slot;
    boolean isAllReady;

    @Override
    public ServerMessageType getType() {
        return ServerMessageType.PLAYER_READY_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);


            dos.writeShort(slot);
            dos.writeBoolean(isAllReady); // writeBoolean() converts boolean to byte automatically

            
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Cannot encoding PlayerReadySend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
