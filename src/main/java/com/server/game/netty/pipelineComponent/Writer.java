package com.server.game.netty.pipelineComponent;


import com.server.game.netty.pipelineComponent.outboundSendMessage.OutboundSendMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;




public class Writer extends  ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Only process OutboundSendMessage messages, if not, pass it to the next handler
        if (!(msg instanceof OutboundSendMessage)) {
            ctx.writeAndFlush(msg, promise);
            return;
        }

        OutboundSendMessage outboundMessage = (OutboundSendMessage) msg;
        System.out.println(">>> Server received a send OutboundSendMessage");

        outboundMessage.send();

        System.out.println(">>> Server processed OutboundSendMessage and sent it to the target(s)");

        System.out.println(">>> =======================================================\n\n");
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage()); 
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
