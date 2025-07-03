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
        new Thread(() -> {
            try {
                nettyServer.start();
            } catch (Exception e) {
                throw new RuntimeException("Failed to start Netty server", e);
            }
        }).start();
    }
}