package com.server.game.netty.messageObject.sendObject;

import java.nio.ByteBuffer;

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
public class MessageSend implements TLVEncodable {
    String message;

    @Override
    public ServerMessageType getType() {
        return ServerMessageType.MESSAGE_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        byte[] messageBytes = Util.stringToBytes(this.message);
        int messageByteLength = messageBytes.length;
        ByteBuffer buf = Util.allocateByteBuffer(Util.INT_SIZE + messageByteLength);
        
        buf.putInt(messageByteLength);
        buf.put(messageBytes);
        return buf.array();
    }


    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
