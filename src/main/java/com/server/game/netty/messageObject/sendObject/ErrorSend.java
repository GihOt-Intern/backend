package com.server.game.netty.messageObject.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.codecableInterface.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.ServerMessageType;
import com.server.game.util.Util;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorSend implements TLVEncodable {
    String errorMessage;

    @Override
    public ServerMessageType getType() { // return enum defined in documentation for this message
        return ServerMessageType.ERROR_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            byte[] errorMessageBytes = Util.stringToBytes(errorMessage);
            int errorMessageByteLength = errorMessageBytes.length;
            dos.writeInt(errorMessageByteLength);
            dos.write(errorMessageBytes);
            
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encoding ErrorSend", e);
        }
    }


    @Override     // this message is only sent to one channel.
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
