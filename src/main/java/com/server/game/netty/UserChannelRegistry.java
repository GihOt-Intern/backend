package com.server.game.netty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;


public class UserChannelRegistry {
    private static final Map<String, Channel> userChannels = new ConcurrentHashMap<>();

    public static final AttributeKey<String> USER_ID = AttributeKey.valueOf("USER_ID");

    public static void register(String userId, Channel channel) {
        if (userChannels.containsKey(userId)) {
            System.out.println(">>> UserId already registered: " + userId);
            return; // UserId already registered, do not register again
        }
        
        // Add userId to channel attributes (to find channel by userId later)
        UserChannelRegistry.setUserId2Channel(userId, channel);
        userChannels.put(userId, channel);
        System.out.println(">>> Registered channel for userId: " + userId);
    }

    public static void unregister(Channel channel) {
       String userId = channel.attr(USER_ID).get();
        if (userId == null) {
            System.out.println(">>> Cannot unregister channel, userId is null.");
            return;
        }
        userChannels.remove(userId);
        System.out.println(">>> Unregistered channel for userId: " + userId);
    }

    public static Channel getChannel(String userId) {
        return userChannels.get(userId);
    }


    private static void setUserId2Channel(String userId, Channel channel) {
        channel.attr(USER_ID).set(userId);
    }
}
