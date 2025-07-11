package com.server.game.netty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;


public class ChannelManager {
    private static final Map<String, Channel> userChannels = new ConcurrentHashMap<>();
    private static final Map<String, Set<Channel>> gameChannels = new ConcurrentHashMap<>();
    private static final Map<String, Set<Channel>> roomChannels = new ConcurrentHashMap<>();

    public static final AttributeKey<String> USER_ID = AttributeKey.valueOf("USER_ID");
    public static final AttributeKey<String> GAME_ID = AttributeKey.valueOf("GAME_ID");
    // public static final AttributeKey<String> ROOM_ID = AttributeKey.valueOf("ROOM_ID");
    public static final AttributeKey<Short> SLOT = AttributeKey.valueOf("SLOT");
    public static final AttributeKey<Boolean> IS_READY = AttributeKey.valueOf("IS_READY");


    public static void register(String userId, String gameId, Channel channel) {
        userRegister(userId, channel);
        gameRegister(gameId, channel);
    }

    // public static void registerToRoom(String userId, String roomId, Channel channel) {
    //     userRegister(userId, channel);
    //     roomRegister(roomId, channel);
    // }

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

    // private static void roomRegister(String roomId, Channel channel) {
    //     if (roomId == null || roomId.isEmpty()) {
    //         System.out.println(">>> Cannot register channel, roomId is null or empty.");
    //         return; // Invalid roomId, do not register
    //     }

    //     // // Add roomId to channel attributes (to find room by channel later)
    //     // ChannelRegistry.setRoomId2Channel(roomId, channel);

    //     // Add the channel to the roomChannels map
    //     roomChannels.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
    //                .add(channel);

    //     System.out.println(">>> Registered channel for roomId: " + roomId);
    // }

    public static void unregister(Channel channel) {
        userUnregister(channel);
        gameUnregister(channel);
        // roomUnregister(channel);
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
            channels.remove(channel);
            if (channels.isEmpty()) {
                gameChannels.remove(gameId); // Remove game entry if no channels left
            }

            System.out.println(">>> Unregistered channel for gameId: " + gameId);
        }
    }

    // private static void roomUnregister(Channel channel) {
    //     String roomId = ChannelRegistry.getRoomIdByChannel(channel);
    //     if (roomId == null) {
    //         System.out.println(">>> Cannot unregister channel, roomId is null.");
    //         return;
    //     }

    //     Set<Channel> channels = roomChannels.get(roomId);
    //     if (channels != null) {
    //         channels.remove(channel);
    //         if (channels.isEmpty()) {
    //             roomChannels.remove(roomId); // Remove room entry if no channels left
    //         }

    //         System.out.println(">>> Unregistered channel for roomId: " + roomId);
    //     }
    // }

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

    public static Set<Channel> getChannelsByRoomId(String roomId) {
        Set<Channel> channelsInRoom = roomChannels.get(roomId);
        if (channelsInRoom == null || channelsInRoom.isEmpty()) {
            System.out.println(">>> No channels found for roomId: " + roomId);
            return Collections.emptySet();
        }
        return channelsInRoom;
    }

    public static String getUserIdByChannel(Channel channel) {
        String userId = channel.attr(USER_ID).get();
        if (userId != null) { return userId; }
        System.out.println(">>> Cannot get userId, it is not set for the channel.");
        return null;
    }

    public static String getGameIdByChannel(Channel channel) {
        String gameId = channel.attr(GAME_ID).get();
        if (gameId != null) { return gameId; }
        System.out.println(">>> Cannot get gameId, it is not set for the channel.");
        return null;
    }

    // public static String getRoomIdByChannel(Channel channel) {
    //     return channel.attr(ROOM_ID).get();
    // }

    public static short getSlotByChannel(Channel channel) {
        Short slot = channel.attr(SLOT).get();
        if (slot == null) {
            System.out.println(">>> Cannot get slot, it is not set for the channel.");
            return -1; // Return -1 if slot is not set
        }
        return slot;
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

    // private static void setRoomId2Channel(String roomId, Channel channel) {
    //     channel.attr(ROOM_ID).set(roomId);
    // }

    public static void setSlot2Channel(short slot, Channel channel) {
        channel.attr(SLOT).set(slot);
    }

    public static void setUserReady(Channel channel) {
        channel.attr(IS_READY).set(true);
    }

    public static void setUserNotReady(Channel channel) {
        channel.attr(IS_READY).set(false);
    }
}
