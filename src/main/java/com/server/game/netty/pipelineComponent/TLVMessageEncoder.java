package com.server.game.netty.pipelineComponent;


import com.server.game.netty.pipelineComponent.outboundSendMessage.OutboundSendMessage;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTargetInterface;
import com.server.game.netty.tlv.codec.TLVEncoder;
import com.server.game.netty.tlv.codecableInterface.TLVEncodable;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;


public class TLVMessageEncoder extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Only process TLVEncodable messages, if not, pass it to the next handler
        if (!(msg instanceof TLVEncodable)) {
            ctx.write(msg, promise); 
            return;
        }

        TLVEncodable sendObject = (TLVEncodable) msg;
        System.out.println(">>> Server received TLVEncodable object: " + sendObject.getClass().getSimpleName());
        System.out.println(">>> Server Encoding TLVEncodable object to ByteBuf");
        byte[] encoded = TLVEncoder.object2Byte(sendObject);

        ByteBuf encodedBuf = Unpooled.wrappedBuffer(encoded);
        System.out.println(">>> Server Encoded TLVEncodable object (" + sendObject.getClass().getSimpleName() + ") to ByteBuf");
        
        SendTargetInterface sendTarget = sendObject.getSendTarget(ctx.channel());

        OutboundSendMessage outboundMessage = new OutboundSendMessage(encodedBuf, sendTarget);
        ctx.write(outboundMessage, promise);
    }

    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace(); 
        ctx.close(); // Close the channel on exception
    }
}
