package com.server.game.exception.socket;

import io.netty.channel.Channel;

import lombok.Getter;

@Getter
public class SocketException extends RuntimeException {
    private Channel channel;

    public SocketException(String message, Channel channel) {
        super(message);
        this.channel = channel;
    }
}
