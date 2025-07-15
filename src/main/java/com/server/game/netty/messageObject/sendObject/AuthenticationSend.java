package com.server.game.netty.messageObject.sendObject;

import java.nio.ByteBuffer;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.ServerMessageType;
import com.server.game.util.Util;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationSend implements TLVEncodable {
    Status statusCode;
    String message;

    @Override
    public ServerMessageType getType() { // return enum defined in documentation for this message
        return ServerMessageType.AUTHENTICATION_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        byte[] messageBytes = Util.stringToBytes(this.message);
        int messageByteLength = messageBytes.length;
        
        ByteBuffer buf = Util.allocateByteBuffer(
            Util.INT_SIZE + Util.INT_SIZE + messageByteLength);
        buf.putInt(statusCode.getStatusCode());
        buf.putInt(messageByteLength);
        buf.put(messageBytes);
        return buf.array();
    }


    @Override     // this message is only sent to one channel.
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }



    @Getter
    @AllArgsConstructor
    public enum Status {
        SUCCESS(8386),
        FAILURE(6838);

        private final int statusCode;
    }
}
