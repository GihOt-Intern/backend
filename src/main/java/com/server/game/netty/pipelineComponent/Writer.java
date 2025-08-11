package com.server.game.netty.pipelineComponent;


import com.server.game.netty.pipelineComponent.outboundSendMessage.OutboundSendMessage;

import io.netty.channel.ChannelFuture;
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
        // System.out.println(">>> [Writer] Server received OutboundSendMessage");
        // System.out.println(">>> [Writer] SendTarget type: " + outboundMessage.getSendTarget().getClass().getSimpleName());

        ChannelFuture future = outboundMessage.send();

        future.addListener(f -> {
            if (f.isSuccess()) {
                // System.out.println(">>> [Writer] Successfully sent OutboundSendMessage to target(s)");
                //System.out.println(">>> =======================================================");
                promise.setSuccess();
            } else {
                System.out.println(">>> [Writer] Failed to send OutboundSendMessage: " + f.cause().getMessage());
                promise.setFailure(f.cause());
            }
        });
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage()); 
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
