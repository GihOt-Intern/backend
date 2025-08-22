package com.server.game.netty.pipelineComponent;


import java.util.HashMap;
import java.util.Map;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageMapping.MessageDispatcher;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BusinessHandler extends SimpleChannelInboundHandler<TLVDecodable> {

    // cannot usse @Autowired here because this class is not a Spring component
    private MessageDispatcher dispatcher;

    public BusinessHandler(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;   
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TLVDecodable receiveObject) throws Exception {

        // Just add known things to contextParams, dispatcher will traverse them and select needed ones
        // to invoke the method
        Map<Class<?>, Object> contextParams = new HashMap<>();
        contextParams.put(String.class, ChannelManager.getUserIdByChannel(ctx.channel()));
        contextParams.put(Channel.class, ctx.channel());
        contextParams.put(ChannelHandlerContext.class, ctx);
        // Add more context parameters as needed
        // ...

        // Use polymorphic dispatching to find the right method to handle the message
        TLVEncodable sendObject = (TLVEncodable) dispatcher.dispatch(receiveObject, contextParams);

        // If handler is void method, sendObject will be null
        if (sendObject == null) {
            return;
        }
        ctx.writeAndFlush(sendObject); // send msg to outbound pipeline
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
