package com.server.game.netty.receiveMessageHandler;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.model.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.ChampionInitialHPsSend;
import com.server.game.netty.messageObject.sendObject.ChampionInitialStatsSend;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend;
import com.server.game.resource.model.Champion;
import com.server.game.resource.model.SlotInfo;
import com.server.game.resource.service.GameStateBuilderService;
import com.server.game.service.GameCoordinator;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameInititalLoadingHandler {

    GameStateBuilderService gameStateBuilderService;
    GameCoordinator gameCoordinator;
    
    // This method is called by LobbyHandler when all players are ready
    public void loadInitial(Channel channel) {
        GameState gameState = gameStateBuilderService.getGameState(channel);
        ChannelFuture future = 
            this.sendInitialPositions(channel, gameState);
        future = this.sendChampionInitialHPs(channel, gameState);
        future = this.sendChampionInitialStats(channel, gameState);   
        
        future.addListener(f -> {
            if (f.isSuccess()) {
                System.out.println(">>> [Log in GameLoadingHandler.loadInitial] Initial loading messages sent successfully.");
                // Send initial game state successfully, register game to gameCoordinator
                gameCoordinator.registerGame(
                    ChannelManager.getGameIdByChannel(channel), 
                    gameState
                );
                String gameId = ChannelManager.getGameIdByChannel(channel);
                this.setSpawnPosition2Cache(gameId, gameState);
            } else {
                System.err.println(">>> [Log in GameLoadingHandler.loadInitial] Initial loading messages failed: " + f.cause());
            }
        });
    }

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
            ChampionInitialStatsSend championInitialStatsSend = 
                new ChampionInitialStatsSend(champion);
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
                    gameState.getSpeed(slot), 
                    System.currentTimeMillis()
                );
            } else {
                System.out.println(">>> [Log in GameLoadingHandler.setSpawnPosition2Cache] Champion with slot " + slot + " not found.");
            }
        }
    }
}