package com.server.game.netty.messageObject.sendObject;

import java.nio.ByteBuffer;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.SendMessageType;
import com.server.game.util.Util;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DistanceSend implements TLVEncodable {
    float distance;

    @Override
    public SendMessageType getType() { // return enum defined in documentation for this message
        return SendMessageType.DISTANCE_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        ByteBuffer buf = Util.allocateByteBuffer(Util.FLOAT_SIZE);
        buf.putFloat(distance);
        return buf.array();
    }

    @Override     // this message is only sent to one channel.
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
