package com.server.game.netty.sendObject;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class IsInPlayGroundSend implements TLVEncodable {

    byte isInPlayGround;

    public IsInPlayGroundSend(boolean isInPlayGround) {
        this.isInPlayGround = (byte) (isInPlayGround ? 1 : 0);
    }

    @Override
    public SendMessageType getType() {
        return SendMessageType.IS_IN_PLAYGROUND_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            dos.writeByte(isInPlayGround);

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Cannot encode IsInPlayGroundSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}