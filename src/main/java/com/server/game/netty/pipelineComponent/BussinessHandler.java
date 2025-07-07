package com.server.game.netty.pipelineComponent;


import com.server.game.netty.messageMapping.MessageDispatcher;
import com.server.game.netty.tlv.codecableInterface.TLVDecodable;
import com.server.game.netty.tlv.codecableInterface.TLVEncodable;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


public class BussinessHandler extends SimpleChannelInboundHandler<TLVDecodable> {

    // cannot usse @Autowired here because this class is not a Spring component
    private MessageDispatcher dispatcher;

    public BussinessHandler(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;   
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TLVDecodable receiveObject) throws Exception {

        System.out.println(">>> Server Received TLVDecodable object: " + receiveObject.getClass().getSimpleName());

        TLVEncodable sendObject = (TLVEncodable) dispatcher.dispatch(receiveObject);
        
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
