package com.server.game.netty.messageHandler;

import java.util.Set;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
                log.info(">>> Initial loading messages sent successfully.");
                // Send initial game state successfully, register game to gameCoordinator
                gameCoordinator.registerGame(gameState);
            } else {
                log.error(">>> Initial loading messages failed: " + f.cause());
            }
        });
    }


    private ChannelFuture sendInitialPositions(Channel channel, GameState gameState) {

        InitialPositionsSend championPositionsSend = 
            new InitialPositionsSend(gameState);
        log.info(">>> Send loading initial positions message");
        return channel.writeAndFlush(championPositionsSend);
    }

    private ChannelFuture sendChampionInitialStats(Channel channel, GameState gameState) {
        // Send message is unicast, need to get all channels in room and send one by one
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);

        ChannelFuture lastFuture = null;
        for (Channel playerChannel : playersInRoom) {
            Short slot = ChannelManager.getSlotByChannel(playerChannel);
            Champion champion = gameState.getChampionBySlot(slot);
            if (champion == null) {
                log.info(">>> Champion with slot " + slot + " not found.");
                continue;
            }
            Integer initGold = gameState.peekGold(slot);


            ChampionInitialStatsSend championInitialStatsSend = 
                new ChampionInitialStatsSend(champion, initGold);

            lastFuture = playerChannel.writeAndFlush(championInitialStatsSend);
        }

        log.info(">>> Sent loading champion initial stats message to all players in the room.");
        return lastFuture == null 
            ? channel.newSucceededFuture() 
            : lastFuture;
    }
}