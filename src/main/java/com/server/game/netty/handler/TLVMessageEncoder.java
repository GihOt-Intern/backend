package com.server.game.netty.handler;

import com.server.game.tlv.codec.TLVEncoder;
import com.server.game.tlv.serializationable.TLVEncodable;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

public class TLVMessageEncoder extends MessageToMessageEncoder<TLVEncodable> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TLVEncodable msg, List<Object> out) throws Exception {
        byte[] encoded = TLVEncoder.object2Byte(msg);
        out.add(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(encoded)));
    }
}
