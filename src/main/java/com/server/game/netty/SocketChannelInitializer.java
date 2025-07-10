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
                65536,
                2, // because the first 2 bytes of a TLVmessage is [type]
                4, // [length] in TLVMessage is 4 bytes
                0,  // 0 because [length] only length of [value], not the whole message length
                0 // no strip, need to keep 3 components: [type], [length] and [value]
            )) 

            // Handle THE FIRST MESSAGE receive from client (must be an authentication message)
            // This handler will be removed after the first message is correct and the user is authenticated
            .addLast(new AuthenticationHandler()) 


            // Receive ByteBuf and convert to TLVDecodable objects  (inbound)
            .addLast(new TLVMessageDecoder()) 
            // Receive ByteBuf, convert it to BinaryWebSocketFrame and send it to the client  (outbound)
            
            .addLast(new Writer()) 
            // Receive TLVEncodable objects and convert them to ByteBuf   (outbound)
            .addLast(new TLVMessageEncoder()) 
            // Business logic handler, receives TLVDecodable objects and creates TLVEncodable objects  (inbound)
            .addLast(new BussinessHandler(dispatcher)) 

            // Handle channel disconnection and unregister user channel (inbound)
            .addLast(new DisconnectHandler()) 
        ;
    }
}
