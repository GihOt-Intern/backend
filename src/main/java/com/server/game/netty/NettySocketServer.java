package com.server.game.netty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.game.util.Util;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NettySocketServer {

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    // This class will be talked specifically in Server-Side-Only documentation
    @Autowired
    private SocketChannelInitializer socketChannelInitializer;


    public synchronized void start() throws Exception {
        
        if (serverChannel != null && serverChannel.isActive()) {
            log.info("WebSocket server is already running.");
            return;
        }

        int port = Util.getNettyServerPort(); // Get the port from .properties or environment variable

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(socketChannelInitializer)
                     .option(ChannelOption.SO_BACKLOG, 128) // Set the backlog size
                     .childOption(ChannelOption.SO_KEEPALIVE,true)
            ;

            ChannelFuture f = bootstrap.bind(port).sync(); 

            this.serverChannel = f.channel();

            log.info("WebSocket server started at host=localhost, port=" + port);


            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("Failed to start WebSocket server: " + e.getMessage());
            throw e;
        }
    }

    
    @PreDestroy   // Shutdown Netty server automatically when Spring server is stopped
    private void stop() throws Exception {
        log.info("Shutting down Netty server...");

        if (serverChannel != null) {
            serverChannel.close().sync();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().sync();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().sync();
        }
        log.info("Netty server shut down.");
    }
}
