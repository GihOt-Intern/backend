package com.server.game.netty;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.server.game.config.SpringContextHolder;
import com.server.game.service.UserService;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;


@Component
public class ChannelManager {

    private static final Map<String, Channel> userChannels = new ConcurrentHashMap<>();
    private static final Map<String, Set<Channel>> gameChannels = new ConcurrentHashMap<>();

    private static final AttributeKey<String>  USER_ID     = AttributeKey.valueOf("USER_ID");
    private static final AttributeKey<String>  USERNAME    = AttributeKey.valueOf("USERNAME");
    private static final AttributeKey<String>  GAME_ID     = AttributeKey.valueOf("GAME_ID");
    private static final AttributeKey<Short>   SLOT        = AttributeKey.valueOf("SLOT");
    private static final AttributeKey<Boolean> IS_READY    = AttributeKey.valueOf("IS_READY");
    private static final AttributeKey<Short>   CHAMPION_ID = AttributeKey.valueOf("CHAMPION_ID");


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
        ChannelManager.setUserId2Channel(userId, channel);

        UserService userService = SpringContextHolder.getBean(UserService.class);
        String username = userService.getUsernameById(userId);
        ChannelManager.setUsername2Channel(username, channel);

        userChannels.put(userId, channel);
        System.out.println(">>> Registered channel for userId: " + userId);
    }

    private static void gameRegister(String gameId, Channel channel) {
        if (gameId == null || gameId.isEmpty()) {
            System.out.println(">>> Cannot register channel, gameId is null or empty.");
            return; // Invalid gameId, do not register
        }

        // Add gameId to channel attributes (to find game by channel later)
        ChannelManager.setGameId2Channel(gameId, channel);


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
       String userId = ChannelManager.getUserIdByChannel(channel);
        if (userId == null) {
            System.out.println(">>> Cannot unregister channel, userId is null.");
            return;
        }
        userChannels.remove(userId); 

        System.out.println(">>> Unregistered channel for userId: " + userId);
    }

    private static void gameUnregister(Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        if (gameId == null) {
            System.out.println(">>> Cannot unregister channel, gameId is null.");
            return;
        }

        Set<Channel> channels = gameChannels.get(gameId);
        if (channels != null) {
            if (!channels.contains(channel)) {
                System.out.println(">>> Channel not found in game channels for gameId: " + gameId);
                return; // Channel not found in the game, nothing to remove
            }

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
        Channel channel = userChannels.get(userId);
        if (channel != null) { return channel; }
        System.out.println(">>> No channel found for userId: " + userId);
        return null; 
    }

    public static Set<Channel> getChannelsByGameId(String gameId) {
        if (!gameChannels.containsKey(gameId)) {
            System.out.println(">>> No channels found for gameId: " + gameId);
            return Collections.emptySet();
        }
        return gameChannels.get(gameId);
    }
    
    public static String getUserIdByChannel(Channel channel) {
        String userId = channel.attr(USER_ID).get();
        if (userId != null) { return userId; }
        System.out.println(">>> Cannot get userId, it is not set for the channel.");
        return null;
    }

    public static String getUsernameByChannel(Channel channel) {
        String username = channel.attr(USERNAME).get();
        if (username != null) { return username; }
        System.out.println(">>> Cannot get username, it is not set for the channel.");
        return null;
    }

    public static String getGameIdByChannel(Channel channel) {
        String gameId = channel.attr(GAME_ID).get();
        if (gameId != null) { return gameId; }
        System.out.println(">>> Cannot get gameId, it is not set for the channel.");
        return null;
    }


    public static Short getSlotByChannel(Channel channel) {
        Short slot = channel.attr(SLOT).get();
        if (slot == null) {
            System.out.println(">>> Cannot get slot, it is not set for the channel.");
            return null; // Return -1 if slot is not set
        }
        return slot;
    }

    public static Short getChampionIdByChannel(Channel channel) {
        Short championId = channel.attr(CHAMPION_ID).get();
        if (championId == null) {
            System.out.println(">>> Cannot get championId, it is not set for the channel.");
            return null; 
        }
        return championId;
    }

    public static Boolean isUserReady(Channel channel) {
        return channel.attr(IS_READY).get();
    }

    private static void setUserId2Channel(String userId, Channel channel) {
        channel.attr(USER_ID).set(userId);
    }

    private static void setGameId2Channel(String gameId, Channel channel) {
        channel.attr(GAME_ID).set(gameId);
    }

    private static void setUsername2Channel(String username, Channel channel) {
        channel.attr(USERNAME).set(username);
    }

    public static void setSlot2Channel(short slot, Channel channel) {
        channel.attr(SLOT).set(slot);
    }
    
    public static void setChampionId2Channel(short championId, Channel channel) {
        channel.attr(CHAMPION_ID).set(championId);
    }

    public static void setUserReady(Channel channel) {
        channel.attr(IS_READY).set(true);
    }

    public static void setUserNotReady(Channel channel) {
        channel.attr(IS_READY).set(false);
    }
}