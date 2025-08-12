package com.server.game.netty;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.server.game.config.SpringContextHolder;
import com.server.game.service.room.RoomRedisService;
import com.server.game.service.scheduler.GameCleanupScheduler;
import com.server.game.service.user.UserService;
import com.server.game.util.ChampionEnum;

import java.util.Collections;
import java.util.HashMap;
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
    private static final AttributeKey<ChampionEnum> CHAMPION_ID = AttributeKey.valueOf("CHAMPION_ID");


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
        ChannelManager.setUserNotReady(channel);


        // Add the channel to the gameChannels map
        gameChannels.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet())
                   .add(channel);

        System.out.println(">>> Registered channel for gameId: " + gameId + "\n\n");
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
                //Remove the room from redis cache
                RoomRedisService roomRedisService = SpringContextHolder.getBean(RoomRedisService.class);
                roomRedisService.deleteById(gameId);
                System.out.println(">>> [Log in gameUnregister()] Removed room from redis cache for roomId: " + gameId);
                
                // Notify GameCleanupService that this game is now empty
                try {
                    GameCleanupScheduler gameCleanupService = 
                        SpringContextHolder.getBean(GameCleanupScheduler.class);
                    gameCleanupService.notifyGameEmpty(gameId);
                } catch (Exception e) {
                    System.out.println(">>> [Warning] Could not notify GameCleanupService for empty game: " + gameId + " - " + e.getMessage());
                }
            }

            System.out.println(">>> Unregistered channel for gameId: " + gameId);
        }
    }


    public static Set<Channel> getAllChannels() {
        return Set.copyOf(userChannels.values());
    }

    public static Set<Channel> getGameChannelsByInnerChannel(Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        return ChannelManager.getChannelsByGameId(gameId);
    }

    public static Channel getAnyChannelByGameId(String gameId) {
        Set<Channel> channels = gameChannels.get(gameId);
        if (channels != null && !channels.isEmpty()) {
            return channels.iterator().next(); // Return any channel from the set
        }
        System.out.println(">>> No channels found for gameId: " + gameId);
        return null; // No channels found for this gameId
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

    public static Channel getChannelByGameIdAndSlot(String gameId, short slot) {
        Set<Channel> channels = ChannelManager.getChannelsByGameId(gameId);
        if (channels != null) {
            for (Channel channel : channels) {
                Short channelSlot = ChannelManager.getSlotByChannel(channel);
                if (channelSlot != null && channelSlot.equals(slot)) {
                    return channel; // Return the first matching channel
                }
            }
        }
        System.out.println(">>> No channel found for gameId: " + gameId + " and slot: " + slot);
        return null; // No matching channel found
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

    public static String getUsernameBySlot(String gameId, short slot) {
        Set<Channel> channels = ChannelManager.getChannelsByGameId(gameId);
        if (channels == null || channels.isEmpty()) {
            System.out.println(">>> No channels found for gameId: " + gameId);
            return null;
        }

        for (Channel channel : channels) {
            Short channelSlot = getSlotByChannel(channel);
            if (channelSlot != null && channelSlot.equals(slot)) {
                return getUsernameByChannel(channel); // Return the username of the matching channel
            }
        }
        System.out.println(">>> No channel found for gameId: " + gameId + " and slot: " + slot);
        return null; // No matching channel found
    }

    public static String getGameIdByChannel(Channel channel) {
        String gameId = channel.attr(GAME_ID).get();
        if (gameId != null) { return gameId; }
        System.out.println(">>> Cannot get gameId, it is not set for the channel.");
        return null;
    }


    public static Short getSlotByChannel(Channel channel) {
        if (channel == null) {
            System.out.println(">>> Cannot get slot, channel is null.");
            return null; // Return -1 if channel is null
        }
        Short slot = 0;
        slot = channel.attr(SLOT).get();
        if (slot == null) {
            System.out.println(">>> Cannot get slot, it is not set for the channel.");
            return null; // Return -1 if slot is not set
        }
        return slot;
    }

    public static ChampionEnum getChampionEnumByChannel(Channel channel) {
        ChampionEnum championEnum = channel.attr(CHAMPION_ID).get();
        if (championEnum == null) {
            System.out.println(">>> [Log in ChannelManager.getChampionEnumByChannel()] Cannot get championEnum, it is not set for the channel.");
            return null;
        }
        return championEnum;
    }
    
    public static ChampionEnum getChampionEnumBySlot(String gameId, short slot) {
        return getSlot2ChampionEnum(gameId).get(slot);
    }

    public static Map<Short, ChampionEnum> getSlot2ChampionEnum(String gameId) {
        Set<Channel> channels = gameChannels.get(gameId);
        if (channels == null || channels.isEmpty()) {
            System.out.println(">>> No channels found for gameId: " + gameId);
            return Collections.emptyMap();
        }

        Map<Short, ChampionEnum> slot2ChampionId = new HashMap<>();
        for (Channel channel : channels) {
            Short slot = getSlotByChannel(channel);
            ChampionEnum championId = getChampionEnumByChannel(channel);
            if (slot != null && championId != null) {
                slot2ChampionId.put(slot, championId);
            }
        }
        return slot2ChampionId;
    }

    public static Boolean isUserReady(Channel channel) {
        Boolean isReady = channel.attr(IS_READY).get();
        if (isReady == null) {
            System.out.println(">>> Cannot get user ready status, it is not set for the channel.");
            return false; // Default to false if not set
        }
        return isReady;
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
    
    public static void setChampionId2Channel(ChampionEnum championId, Channel channel) {
        channel.attr(CHAMPION_ID).set(championId);
    }

    public static void setUserReady(Channel channel) {
        channel.attr(IS_READY).set(true);
    }

    public static void setUserNotReady(Channel channel) {
        channel.attr(IS_READY).set(false);
    }

    public static void removeSlotMapping(String gameId, short slot) {
        if (gameChannels.containsKey(gameId)) {
            Set<Channel> channels = gameChannels.get(gameId);
            channels.removeIf(channel -> getSlotByChannel(channel) == slot);
            if (channels.isEmpty()) {
                gameChannels.remove(gameId);
            }
        }
    }


    public static void clearGameSlotMappings(String gameId) {
        if (gameChannels.containsKey(gameId)) {
            Set<Channel> channels = gameChannels.get(gameId);
            channels.forEach(channel -> {
                channel.attr(SLOT).set(null);
            });
        }
    }

    /**
     * Get all user channels as a map of userId to Channel
     */
    public static Map<String, Channel> getAllUserChannels() {
        return Collections.unmodifiableMap(userChannels);
    }
}