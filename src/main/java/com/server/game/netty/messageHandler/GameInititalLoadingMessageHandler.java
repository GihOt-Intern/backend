package com.server.game.netty.messageHandler;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.server.game.factory.GameStateFactory;
import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.initialGameState.ChampionInitialStatsSend;
import com.server.game.netty.sendObject.initialGameState.InitialPositionsSend;
import com.server.game.service.gameState.GameCoordinator;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameInititalLoadingMessageHandler {

    GameStateFactory gameStateBuilder;
    GameCoordinator gameCoordinator;
    
    // This method is called by LobbyHandler when all players are ready
    public void loadInitial(Channel channel) {
        GameState gameState = gameStateBuilder.createGameState(channel);
        ChannelFuture future = 
            this.sendInitialPositions(channel, gameState);
        future = this.sendChampionInitialStats(channel, gameState);
        
        future.addListener(f -> {
            if (f.isSuccess()) {
                System.out.println(">>> [Log in GameLoadingHandler.loadInitial] Initial loading messages sent successfully.");
                // Send initial game state successfully, register game to gameCoordinator
                gameCoordinator.registerGame(gameState);
            } else {
                System.err.println(">>> [Log in GameLoadingHandler.loadInitial] Initial loading messages failed: " + f.cause());
            }
        });
    }


    private ChannelFuture sendInitialPositions(Channel channel, GameState gameState) {

        InitialPositionsSend championPositionsSend = 
            new InitialPositionsSend(gameState);
        System.out.println(">>> Send loading initial positions message");
        return channel.writeAndFlush(championPositionsSend);
    }

    private ChannelFuture sendChampionInitialStats(Channel channel, GameState gameState) {
        // Send message is unicast, need to get all channels in room and send one by one
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        
        // Get champions' initial HPs
        Map<String, Integer> allInitHPs = gameState.getChampions()
            .stream().collect(
                Collectors.toMap(Champion::getStringId, Champion::getInitialHP));

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
}