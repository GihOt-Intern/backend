package com.server.game.netty.messageObject.sendObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTargetInterface;
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
public class MessageSend implements TLVEncodable {
    String message;

    @Override
    public ServerMessageType getType() {
        return ServerMessageType.MESSAGE_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        byte[] messageBytes = message.getBytes(Charset.forName("UTF-32BE"));
        int messageLength = messageBytes.length;
        ByteBuffer buf = ByteBuffer.allocate(Util.STRING_BYTE_SIZE(messageLength)).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(messageLength);
        buf.put(messageBytes);
        return buf.array();
    }


    @Override
    public SendTargetInterface getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
