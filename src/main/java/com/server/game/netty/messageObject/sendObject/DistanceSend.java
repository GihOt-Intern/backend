package com.server.game.netty.messageObject.sendObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.server.game.netty.ChannelRegistry;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTargetInterface;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.RoomBroadcastTarget;
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
public class DistanceSend implements TLVEncodable {
    Double distance;

    @Override
    public ServerMessageType getType() {
        return ServerMessageType.DISTANCE_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        ByteBuffer buf = ByteBuffer.allocate(Util.DOUBLE_SIZE).order(ByteOrder.BIG_ENDIAN);
        buf.putDouble(distance);
        return buf.array();
    }

    @Override
    public SendTargetInterface getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
