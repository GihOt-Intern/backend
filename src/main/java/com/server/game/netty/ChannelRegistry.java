package com.server.game.netty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;


public class ChannelRegistry {
    private static final Map<String, Channel> userChannels = new ConcurrentHashMap<>();
    private static final Map<String, Set<Channel>> gameChannels = new ConcurrentHashMap<>();

    public static final AttributeKey<String> USER_ID = AttributeKey.valueOf("USER_ID");
    public static final AttributeKey<String> GAME_ID = AttributeKey.valueOf("GAME_ID");
    public static final AttributeKey<Short> SLOT = AttributeKey.valueOf("SLOT");


    public static void register(String userId, String gameId, Channel channel) {
        userRegister(userId, channel);
        gameRegister(gameId, channel);
    }

    private static void userRegister(String userId, Channel channel) {
        if (userId == null || userId.isEmpty()) {
            System.out.println(">>> Cannot register channel, userId is null or empty.");
            return; // Invalid userId, do not register
        }

        if (userChannels.containsKey(userId)) {
            System.out.println(">>> UserId already registered: " + userId);
            return; // UserId already registered, do not register again
        }
        
        // Add userId to channel attributes (to find channel by userId later)
        ChannelRegistry.setUserId2Channel(userId, channel);

        userChannels.put(userId, channel);
        System.out.println(">>> Registered channel for userId: " + userId);
    }

    private static void gameRegister(String gameId, Channel channel) {
        if (gameId == null || gameId.isEmpty()) {
            System.out.println(">>> Cannot register channel, gameId is null or empty.");
            return; // Invalid gameId, do not register
        }

        // Add gameId to channel attributes (to find game by channel later)
        ChannelRegistry.setGameId2Channel(gameId, channel);

        // Add the channel to the gameChannels map
        gameChannels.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet())
                   .add(channel);

        System.out.println(">>> Registered channel for gameId: " + gameId);
    }

    

    public static void unregister(Channel channel) {
        userUnregister(channel);
        gameUnregister(channel);
    }


    private static void userUnregister(Channel channel) {
       String userId = ChannelRegistry.getUserIdByChannel(channel);
        if (userId == null) {
            System.out.println(">>> Cannot unregister channel, userId is null.");
            return;
        }
        userChannels.remove(userId);

        System.out.println(">>> Unregistered channel for userId: " + userId);
    }


    private static void gameUnregister(Channel channel) {
        String gameId = ChannelRegistry.getGameIdByChannel(channel);
        if (gameId == null) {
            System.out.println(">>> Cannot unregister channel, gameId is null.");
            return;
        }

        Set<Channel> channels = gameChannels.get(gameId);
        if (channels != null) {
            channels.remove(channel);
            if (channels.isEmpty()) {
                gameChannels.remove(gameId); // Remove game entry if no channels left
            }

            System.out.println(">>> Unregistered channel for gameId: " + gameId);
        }
    }

    public static Set<Channel> getAllChannels() {
        return Set.copyOf(userChannels.values());
    }

    public static Channel getChannelByUserId(String userId) {
        return userChannels.get(userId);
    }

    public static Set<Channel> getChannelsByGameId(String gameId) {
        if (!gameChannels.containsKey(gameId)) {
            System.out.println(">>> No channels found for gameId: " + gameId);
            return Collections.emptySet();
        }
        return gameChannels.get(gameId);
    }

    public static String getUserIdByChannel(Channel channel) {
        return channel.attr(USER_ID).get();
    }

    public static String getGameIdByChannel(Channel channel) {
        return channel.attr(GAME_ID).get();
    }

    public static short getSlotByChannel(Channel channel) {
        Short slot = channel.attr(SLOT).get();
        if (slot == null) {
            System.out.println(">>> Cannot get slot, it is not set for the channel.");
            return -1; // Return -1 if slot is not set
        }
        return slot;
    }

    private static void setUserId2Channel(String userId, Channel channel) {
        channel.attr(USER_ID).set(userId);
    }

    private static void setGameId2Channel(String gameId, Channel channel) {
        channel.attr(GAME_ID).set(gameId);
    }

    public static void setSlot2Channel(short slot, Channel channel) {
        channel.attr(SLOT).set(slot);
    }
}
