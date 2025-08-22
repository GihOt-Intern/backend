package com.server.game.netty.pipelineComponent;

import java.util.List;

import com.server.game.netty.sendObject.MessageSend;
import com.server.game.netty.tlv.messageEnum.ReceiveMessageType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthenticationHandler extends MessageToMessageDecoder<ByteBuf> {


    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        
        // peek only, do not move the reader index
        short type = buf.getShort(buf.readerIndex());
        
        // If this first message is not an authentication message, close the channel 
        if (type != ReceiveMessageType.AUTHENTICATION_RECEIVE.getType()) {
            log.error(">>> Invalid: First message from client is not an authentication message");

            // Create a MessageSend (concrete of TLVEncodable) and flush to pipeline
            // to come to TVLMessageEncoder. That handler will send the error message to client 
            MessageSend errorMessage = new MessageSend("Invalid first message, expected authentication message.");
            ctx.channel().writeAndFlush(errorMessage);
            return;
        }

        // Send message to next handler in pipeline, BussinessHandler will catch, dispatch this message
        // and handle authentication later. In authenticate method, if successful, it will remove this handler from the pipeline

        out.add(buf.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
