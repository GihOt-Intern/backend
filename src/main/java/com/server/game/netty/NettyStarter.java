package com.server.game.netty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.event.ApplicationReadyEvent;

@Slf4j
@Component
public class NettyStarter {

    @Autowired
    private NettySocketServer nettyServer;

    @EventListener(ApplicationReadyEvent.class)
    public void startNetty() {

        new Thread(() -> {
            try {
                nettyServer.start();
            } catch (Exception e) {
                log.info("Failed to start Netty server.");
                e.printStackTrace();
                throw new RuntimeException("Failed to start Netty server", e);
            }
        }, "Netty-Starter-Thread").start();
    }
}
