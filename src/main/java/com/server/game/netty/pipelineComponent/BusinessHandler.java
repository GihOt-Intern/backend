package com.server.game.netty.pipelineComponent;


import java.util.HashMap;
import java.util.Map;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageMapping.MessageDispatcher;
import com.server.game.netty.tlv.codecableInterface.TLVDecodable;
import com.server.game.netty.tlv.codecableInterface.TLVEncodable;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


public class BusinessHandler extends SimpleChannelInboundHandler<TLVDecodable> {

    // cannot usse @Autowired here because this class is not a Spring component
    private MessageDispatcher dispatcher;

    public BusinessHandler(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;   
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TLVDecodable receiveObject) throws Exception {

        System.out.println(">>> Server Received TLVDecodable object: " + receiveObject.getClass().getSimpleName());

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

        if (sendObject == null) {
            System.out.println(">>> This message no need to send back response to client, stop pipeline.");
            return;
        }
        System.out.println(">>> Server Created TLVEncodable object of type: " + sendObject.getClass().getSimpleName());
        ctx.writeAndFlush(sendObject); // send msg to outbound pipeline
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
