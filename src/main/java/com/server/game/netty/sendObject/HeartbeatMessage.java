package com.server.game.netty.sendObject;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;
import lombok.NoArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

@NoArgsConstructor
public class HeartbeatMessage implements TLVEncodable {

    @Override
    public SendMessageType getType() {
        return SendMessageType.HEARTBEAT_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Heartbeat message is empty, so we just return an empty byte array
            dos.writeInt(0); // Length of the message is 0

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Cannot encode HeartbeatMessage", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}