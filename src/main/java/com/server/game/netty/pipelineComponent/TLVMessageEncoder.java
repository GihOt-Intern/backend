package com.server.game.netty.pipelineComponent;


import com.server.game.netty.pipelineComponent.outboundSendMessage.OutboundSendMessage;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.sendObject.PongSend;
import com.server.game.netty.tlv.codec.TLVEncoder;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;

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
            ctx.writeAndFlush(msg, promise); 
            return;
        }

        TLVEncodable sendObject = (TLVEncodable) msg; // cast msg to TLVEncodable
        if (!(sendObject instanceof PongSend)) {
            System.out.println(">>> [TLVMessageEncoder] Server received TLVEncodable object: " + sendObject.getClass().getSimpleName());
            System.out.println(">>> [TLVMessageEncoder] Message type: " + sendObject.getType());
        }

        try {
            //System.out.println(">>> Server Encoding TLVEncodable object to ByteBuf");
            byte[] encoded = TLVEncoder.object2Bytes(sendObject);

            ByteBuf encodedBuf = Unpooled.wrappedBuffer(encoded); // wrap the byte array into a ByteBuf
            // System.out.println(">>> Server Encoded TLVEncodable object (" + sendObject.getClass().getSimpleName() + ") to ByteBuf");
            // System.out.println(">>> Send byte: ");
            // ByteBuffer byteBuffer = encodedBuf.nioBuffer();
            // Util.printHex(byteBuffer, true);
            // Get send target from the TLVEncodable object
            SendTarget sendTarget = sendObject.getSendTarget(ctx.channel());

            // Wrap ByteBuf and send target into an object to send to the next handler in pipeline
            OutboundSendMessage outboundMessage = new OutboundSendMessage(encodedBuf, sendTarget);
            ctx.writeAndFlush(outboundMessage, promise);
        } catch (Exception e) {
            System.out.println(">>> [TLVMessageEncoder] ERROR during encoding: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace(); 
        ctx.close(); // Close the channel on exception
    }
}
