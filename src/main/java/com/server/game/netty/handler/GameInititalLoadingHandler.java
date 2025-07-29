package com.server.game.netty.handler;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.initialGameState.ChampionInitialHPsSend;
import com.server.game.netty.sendObject.initialGameState.ChampionInitialStatsSend;
import com.server.game.netty.sendObject.initialGameState.InitialPositionsSend;
import com.server.game.resource.model.SlotInfo;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.gameState.GameStateBuilder;
import com.server.game.service.gameState.GameStateManager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameInititalLoadingHandler {

    GameStateBuilder gameStateBuilder;
    @Lazy
    GameCoordinator gameCoordinator;
    GameStateManager gameStateManager;
    
    // This method is called by LobbyHandler when all players are ready
    public void loadInitial(Channel channel) {
        GameState gameState = gameStateBuilder.build(channel);
        ChannelFuture future = 
            this.sendInitialPositions(channel, gameState);
        future = this.sendChampionInitialHPs(channel, gameState);
        future = this.sendChampionInitialStats(channel, gameState);

        // Initialize game state before completing
        String gameId = ChannelManager.getGameIdByChannel(channel);

    
        boolean isInitSuccess = gameStateManager.initializeGame(gameState);

        if (isInitSuccess) {
            System.out.println(">>> [Log in GameLoadingHandler.initializeGameState] Successfully initialized game state for gameId: " + gameId);
        } else {
            System.err.println(">>> [Log in GameLoadingHandler.initializeGameState] Failed to initialize game state for gameId: " + gameId);
        }
        
        future.addListener(f -> {
            if (f.isSuccess()) {
                System.out.println(">>> [Log in GameLoadingHandler.loadInitial] Initial loading messages sent successfully.");
                // Send initial game state successfully, register game to gameCoordinator
                gameCoordinator.registerGame(
                    ChannelManager.getGameIdByChannel(channel), 
                    gameState
                );
                this.setSpawnPosition2Cache(gameId, gameState);
            } else {
                System.err.println(">>> [Log in GameLoadingHandler.loadInitial] Initial loading messages failed: " + f.cause());
            }
        });
    }

    /**
     * Initialize game state for the given gameId
     * @param gameId
     * @param gameState
     * @return
     */
    // private void initializeGameState(String gameId, GameState gameState) {
    //     Map<Short, ChampionEnum> slot2Champion = new HashMap<>();
        
    //     // Populate slot to champion map from game state
    //     for (Map.Entry<Short, Champion> entry : gameState.getChampions().entrySet()) {
    //         Short slot = entry.getKey();
    //         Champion champion = entry.getValue();
    //         ChampionEnum championEnum = ChampionEnum.fromShort(champion.getId());
    //         slot2Champion.put(slot, championEnum);
    //         System.out.println(">>> Adding champion " + championEnum + " for slot " + slot);
    //     }

    //     Map<ChampionEnum, Integer> championInitialHPMap = new HashMap<>();
    //     for (ChampionEnum championId : slot2Champion.values()) {
    //         Integer initialHP = championService.getInitialHP(championId);
    //         championInitialHPMap.put(championId, initialHP);
    //         System.out.println(">>> Setting initial HP " + initialHP + " for champion " + championId);
    //     }

    //     boolean initSuccess = gameStateManager.initializeGame(gameId, slot2Champion, championInitialHPMap);

    //     if (initSuccess) {
    //         System.out.println(">>> [Log in GameLoadingHandler.initializeGameState] Successfully initialized game state for gameId: " + gameId);
    //     } else {
    //         System.err.println(">>> [Log in GameLoadingHandler.initializeGameState] Failed to initialize game state for gameId: " + gameId);
    //     }
    // }



    private ChannelFuture sendInitialPositions(Channel channel, GameState gameState) {
        List<SlotInfo> slotInfos = gameState.getSlotInfos();

        InitialPositionsSend championPositionsSend = 
            new InitialPositionsSend(gameState.getGameMapId(), slotInfos);
        System.out.println(">>> Send loading initial positions message");
        return channel.writeAndFlush(championPositionsSend);
    }

    private ChannelFuture sendChampionInitialHPs(Channel channel, GameState gameState) {
        Map<Short, Champion> slot2Champion = gameState.getChampions();
        ChampionInitialHPsSend championInitialHPsSend = 
                            new ChampionInitialHPsSend(slot2Champion);
        System.out.println(">>> Send loading champion initial HPs message");
        return channel.writeAndFlush(championInitialHPsSend);
    }

    private ChannelFuture sendChampionInitialStats(Channel channel, GameState gameState) {
        // Send message is unicast, need to get all channels in room and send one by one
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        ChannelFuture lastFuture = null;
        for (Channel playerChannel : playersInRoom) {
            Short slot = ChannelManager.getSlotByChannel(playerChannel);
            Champion champion = gameState.getChampionBySlot(slot);
            if (champion == null) {
                System.out.println(">>> [Log in MapHandler.handleInitialGameStateLoading] Champion with slot " + slot + " not found.");
                continue;
            }
            Integer initGold = gameState.peekGold(slot);
            ChampionInitialStatsSend championInitialStatsSend = 
                new ChampionInitialStatsSend(champion, initGold);
            lastFuture = playerChannel.writeAndFlush(championInitialStatsSend);
        }

        System.out.println(">>> Sent loading champion initial stats message to all players in the room.");
        return lastFuture == null 
            ? channel.newSucceededFuture() 
            : lastFuture;
    }

    private void setSpawnPosition2Cache(String gameId, GameState gameState) {
        // Set spawn position for each player in the game state
        for (Short slot : gameState.getChampions().keySet()) {
            Champion champion = gameState.getChampionBySlot(slot);
            if (champion != null) {
                gameCoordinator.updatePosition(
                    gameId, 
                    slot, 
                    gameState.getSpawnPosition(slot), 
                    champion.getMoveSpeed(), 
                    System.currentTimeMillis()
                );
            } else {
                System.out.println(">>> [Log in GameLoadingHandler.setSpawnPosition2Cache] Champion with slot " + slot + " not found.");
            }
        }
    }
}