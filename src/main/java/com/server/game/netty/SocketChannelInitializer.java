package com.server.game.netty;

import com.server.game.netty.pipelineComponent.*;
import com.server.game.netty.messageMapping.MessageDispatcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;


@Component
public class SocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    // Will be talked later
    @Autowired
    private MessageDispatcher dispatcher;

    @Override
    public void initChannel(SocketChannel ch) throws Exception { 

        ch.pipeline()
            // Ensure the incoming ByteBuf is a complete frame
            .addLast(new LengthFieldBasedFrameDecoder(
                65536, // max frame length
                2,  // offset, because the first 2 bytes of a TLVmessage is [type]
                4,  // [length] in TLVMessage is int, 4 bytes
                0,   // 0 because [length] is only the length of [value], not the whole message
                0 // no strip, need to keep 3 components: [type], [length] and [value]
            )) 

            // Handle THE FIRST MESSAGE receive from client (must be an authentication message)
            // This handler will be removed from pipeline if the first message 
            // is correct and the user is authenticated                              (inbound)
            .addLast(new AuthenticationHandler()) 

            // Receive ByteBuf and convert to TLVDecodable objects                   (inbound)
            .addLast(new TLVMessageDecoder()) 

            // Receive ByteBuf, and send it to needed channel(s)                     (outbound)
            .addLast(new Writer()) 
            // Receive TLVEncodable objects and convert them to ByteBuf              (outbound)
            .addLast(new TLVMessageEncoder()) 
            // Application layer, receives TLVDecodable objects, processes,
            // creates TLVEncodable objects (if any), flushes to outbound pipeline   (inbound)
            .addLast(new BusinessHandler(dispatcher)) 

            // Handle channel disconnection and unregister user channel              (inbound)
            .addLast(new DisconnectHandler()) 
        ;
    }
}
