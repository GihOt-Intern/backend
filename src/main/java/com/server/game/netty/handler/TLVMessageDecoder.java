package com.server.game.netty.handler;

import com.server.game.tlv.codec.TLVDecoder;
import com.server.game.tlv.serializationable.TLVDecodable;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

public class TLVMessageDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame frame, List<Object> out) throws Exception {
        System.out.println("Decoding BinaryWebSocketFrame to TLVDecodable object");

        ByteBuf buf = frame.content();

        if (buf.readableBytes() < 6) return;

        buf.markReaderIndex();

        short type = buf.readShort();
        int length = buf.readInt();

        if (buf.readableBytes() < length) {
            buf.resetReaderIndex();
            return;
        }

        byte[] value = new byte[length];
        buf.readBytes(value);

        TLVDecodable receiveObject = TLVDecoder.byte2Object(type, value);
        out.add(receiveObject);

        System.out.println("Decoded TLVDecodable object: " + receiveObject.getClass().getSimpleName());
    }
}
