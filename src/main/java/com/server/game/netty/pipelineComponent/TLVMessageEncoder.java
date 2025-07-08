package com.server.game.netty.pipelineComponent;

import com.server.game.netty.tlv.codec.TLVEncoder;
import com.server.game.netty.tlv.codecableInterface.TLVEncodable;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


public class TLVMessageEncoder extends MessageToByteEncoder<TLVEncodable> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TLVEncodable sendObject, ByteBuf out) throws Exception {
        System.out.println(">>> Server Encoding TLVEncodable object to ByteBuf");
        byte[] encoded = TLVEncoder.object2Byte(sendObject);

        ByteBuf encodedBuf = Unpooled.wrappedBuffer(encoded);
        System.out.println(">>> Server Encoded TLVEncodable object (" + sendObject.getClass().getSimpleName() + ") to ByteBuf");
        out.writeBytes(encodedBuf);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace(); 
        ctx.close(); // Close the channel on exception
    }
}
