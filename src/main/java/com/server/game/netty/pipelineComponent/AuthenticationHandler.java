package com.server.game.netty.pipelineComponent;

import java.util.List;

import com.server.game.netty.tlv.typeDefine.ClientMessageType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;


public class AuthenticationHandler extends MessageToMessageDecoder<ByteBuf> {


    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        short type = buf.readShort();
        // If this first message is not an authentication message, close the channel 
        if (type != ClientMessageType.AUTHENTICATION_RECEIVE.getType()) {
            System.out.println(">>> Invalid: First message from client is not an authentication message, closing channel.");
            ctx.close();
            return;
        }

        System.out.println(">>> Server received first message is the authentication message, processing...");

        // Reset buffer index to preserve the message for further processing
        buf.resetReaderIndex();

        // Remove this handler from the pipeline.
        // Authentication will be handled in BussinessHandler
        ctx.pipeline().remove(this);

        // Send message to next handler in pipeline, BussinessHandler will dispatch the message
        // and handle authentication later
        out.add(buf.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
