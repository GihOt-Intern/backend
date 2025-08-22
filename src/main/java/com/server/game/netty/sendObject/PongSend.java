package com.server.game.netty.sendObject;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

@AllArgsConstructor
public class PongSend implements TLVEncodable {

    long timestamp;

    public PongSend() {
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public SendMessageType getType() {
        return SendMessageType.PONG_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeLong(timestamp);

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Cannot encode PongSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}