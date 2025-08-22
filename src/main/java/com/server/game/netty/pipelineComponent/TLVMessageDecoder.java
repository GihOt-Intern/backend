package com.server.game.netty.pipelineComponent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import com.server.game.netty.tlv.codec.TLVDecoder;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;

@Slf4j
public class TLVMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        // Util.printHex(buf.nioBuffer(), true);

        short type = buf.readShort();
        int length = buf.readInt();

        if (buf.readableBytes() != length) {
            return;
        }

        byte[] value = new byte[length];
        buf.readBytes(value);

        TLVDecodable receiveObject = TLVDecoder.bytes2Object(type, value);

        out.add(receiveObject);            
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(">>> Server Exception: " + cause.getMessage(), cause);
        ctx.close(); // Close the channel on exception
    }
}
