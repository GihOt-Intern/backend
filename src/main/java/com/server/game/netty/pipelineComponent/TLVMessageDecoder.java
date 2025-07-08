package com.server.game.netty.pipelineComponent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.ByteBuffer;
import java.util.List;

import com.server.game.netty.tlv.codec.TLVDecoder;
import com.server.game.netty.tlv.codecableInterface.TLVDecodable;
import com.server.game.util.Util;

public class TLVMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        System.out.println(">>> Server Decoding ByteBuf to TLVDecodable object");

        // while (buf.readableBytes() >= 6) {
        //     buf.markReaderIndex();
        System.out.println(">>> Server received ByteBuf:");
        Util.printHex(buf.nioBuffer());

        short type = buf.readShort();
        int length = buf.readInt();

        System.out.println(">>> Server Read TLV type: " + type + ", length: " + length);

        if (buf.readableBytes() < length) {
            System.out.println(">>> Server Not enough bytes to read for type: " + type + ", resetting reader index.");
            buf.resetReaderIndex();
            // break;
        }

        // byte[] value = new byte[length];
        // buf.readBytes(value);

        // Convert from io.netty.buffer.ByteBuf to java.nio.ByteBuffer
        ByteBuffer buffer = buf.nioBuffer();

        TLVDecodable receiveObject = TLVDecoder.byte2Object(type, buffer);
        System.out.println(">>> Server Decoded TLVDecodable object: " + receiveObject.getClass().getSimpleName());

        out.add(receiveObject);            
        // }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
