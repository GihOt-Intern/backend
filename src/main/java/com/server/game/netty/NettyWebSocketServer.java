package com.server.game.netty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;

@Component
public class NettyWebSocketServer {

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Autowired
    private WebSocketChannelInitializer webSocketChannelInitializer;


    public synchronized void start() throws Exception {
        
        if (serverChannel != null && serverChannel.isActive()) {
            System.out.println("WebSocket server is already running.");
            return;
        }
        
        int port = 8386;

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(webSocketChannelInitializer)
                     .option(ChannelOption.SO_BACKLOG, 128) // Set the backlog size
                     .childOption(ChannelOption.SO_KEEPALIVE,true);

            ChannelFuture f = bootstrap.bind(port).sync();;

            this.serverChannel = f.channel();


            System.out.println("WebSocket server started at ws://localhost:" + port + "/ws");


            f.channel().closeFuture().sync();
        } catch (Exception e) {
            System.err.println("Failed to start WebSocket server: " + e.getMessage());
            throw e;
        } 
        // finally {
        //     this.stop();
        // }
    }

    @PreDestroy
    private void stop() throws Exception {
        System.out.println("Shutting down Netty server...");

        if (serverChannel != null) {
            serverChannel.close().sync();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().sync();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().sync();
        }
        System.out.println("Netty server shut down.");
    }
}
