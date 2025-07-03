package com.server.game.netty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NettyStarter {

    private static final Logger log = LoggerFactory.getLogger(NettyStarter.class);
    private volatile boolean started = false;

    @Autowired
    private NettyWebSocketServer nettyServer;

    @EventListener(ApplicationReadyEvent.class)
    public void startNetty() {

        System.out.println("NettyStarter -> Starting Netty...");

        if (started) {
            log.warn("Netty server already started. Skipping.");
            return;
        }

        new Thread(() -> {
            try {
                nettyServer.start();
                started = true;
                log.info("Netty server started successfully.");
            } catch (Exception e) {
                log.error("Failed to start Netty server. Port may be in use or server already running.", e);
                // Optionally don't crash entire app:
                // return;
                throw new RuntimeException("Failed to start Netty server", e);
            }
        }, "Netty-Starter-Thread").start();
    }
}
