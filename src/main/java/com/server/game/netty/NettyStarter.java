package com.server.game.netty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class NettyStarter {

    @Autowired
    private NettyWebSocketServer nettyServer;

    @EventListener(ApplicationReadyEvent.class)
    public void startNetty() {

        System.out.println("NettyStarter -> Starting Netty...");

        new Thread(() -> {
            try {
                nettyServer.start();
                System.out.println("Netty server started successfully.");
            } catch (Exception e) {
                System.out.println("Failed to start Netty server. Port may be in use or server already running.");
                e.printStackTrace();
                throw new RuntimeException("Failed to start Netty server", e);
            }
        }, "Netty-Starter-Thread").start();
    }
}
