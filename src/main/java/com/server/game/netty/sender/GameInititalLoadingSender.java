package com.server.game.netty.sender;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.server.game.factory.GameStateFactory;
import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.initialGameState.ChampionInitialHPsSend;
import com.server.game.netty.sendObject.initialGameState.ChampionInitialStatsSend;
import com.server.game.netty.sendObject.initialGameState.InitialPositionsSend;
import com.server.game.resource.model.SlotInfo;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.gameState.GameStateManager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameInititalLoadingSender {

    GameStateFactory gameStateBuilder;
    GameCoordinator gameCoordinator;
    GameStateManager gameStateManager;
    
    // This method is called by LobbyHandler when all players are ready
    public void loadInitial(Channel channel) {
        GameState gameState = gameStateBuilder.createGameState(channel);
        ChannelFuture future = 
            this.sendInitialPositions(channel, gameState);
        // future = this.sendChampionInitialHPs(channel, gameState);
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


    private ChannelFuture sendInitialPositions(Channel channel, GameState gameState) {
        List<SlotInfo> slotInfos = gameState.getSlotInfos();

        InitialPositionsSend championPositionsSend = 
            new InitialPositionsSend(gameState);
        System.out.println(">>> Send loading initial positions message");
        return channel.writeAndFlush(championPositionsSend);
    }

    @Deprecated
    @SuppressWarnings("unused")
    // This method is deprecated, do not use it anymore
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
        
        // Get champions' initial HPs
        Map<Short, Integer> allInitHPs = gameState.getChampions()
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, 
                    entry -> entry.getValue().getInitialHP()));
        
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
                new ChampionInitialStatsSend(champion, initGold, allInitHPs);

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
                    gameState.getSpawnPosition(gameState.getSlotState(slot)), 
                    champion.getMoveSpeed(), 
                    System.currentTimeMillis()
                );
            } else {
                System.out.println(">>> [Log in GameLoadingHandler.setSpawnPosition2Cache] Champion with slot " + slot + " not found.");
            }
        }
    }
}