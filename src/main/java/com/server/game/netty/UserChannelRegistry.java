package com.server.game.netty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import io.netty.channel.Channel;


public class UserChannelRegistry {
    private static final Map<String, Channel> userChannels = new ConcurrentHashMap<>();

    public static void register(String userId, Channel channel) {
        userChannels.put(userId, channel);
    }

    public static void unregister(String userId) {
        userChannels.remove(userId);
    }

    public static Channel getChannel(String userId) {
        return userChannels.get(userId);
    }
}
