package com.server.game.netty.receiveMessageHandler;


import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend.InitialPositionData;
import com.server.game.netty.messageObject.sendObject.ChampionInitialHPsSend;
import com.server.game.netty.messageObject.sendObject.ChampionInitialHPsSend.ChampionInitialHPData;
import com.server.game.netty.messageObject.sendObject.ChampionInitialStatsSend;
import com.server.game.resource.model.Champion;
import com.server.game.resource.service.ChampionService;
import com.server.game.resource.service.GameMapService;
import com.server.game.util.ChampionEnum;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MapHandler {

    GameMapService gameMapService;
    ChampionService championService;

    // This method is called by LobbyHandler when all players are ready
    public List<InitialPositionData> handleInitialGameStateLoading(Channel channel) {
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        Short numPlayers = (short) playersInRoom.size();

        // GameMap id is determined by the number of players
        Short gameMapId = numPlayers;

        List<InitialPositionData> initialPositionDatas = this.handleInitialPositionsLoading(channel, gameMapId);        
        this.handleChampionInitialHPsLoading(channel);
        this.handleChampionInitialStatsLoading(channel);

        return initialPositionDatas;
    }


    private List<InitialPositionData> handleInitialPositionsLoading(Channel channel, Short gameMapId) {
        List<InitialPositionData> initialPositionsData = 
            gameMapService.getInitialPositionsData(gameMapId);

        InitialPositionsSend championPositionsSend = 
            new InitialPositionsSend(gameMapId, initialPositionsData);
        System.out.println(">>> Send loading initial positions message");
        channel.writeAndFlush(championPositionsSend);     
        return initialPositionsData;
    }

    private ChannelFuture handleChampionInitialHPsLoading(Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        List<ChampionInitialHPData> initialHPsData = 
            championService.getChampionInitialHPsData(gameId);

        ChampionInitialHPsSend championInitialHPsSend = 
            new ChampionInitialHPsSend(initialHPsData);
        System.out.println(">>> Send loading initial HPs message");
        return channel.writeAndFlush(championInitialHPsSend);     
    }

    private ChannelFuture handleChampionInitialStatsLoading(Channel channel) {
        // Send message is unicast, need to get all channels in room and send one by one
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        ChannelFuture lastFuture = null;
        for (Channel playerChannel : playersInRoom) {
            ChampionEnum championId = ChannelManager.getChampionIdByChannel(playerChannel);
            Champion champion = championService.getChampionById(championId);
            if (champion != null) {
                ChampionInitialStatsSend championInitialStatsSend = 
                    new ChampionInitialStatsSend(champion);
                System.out.println(">>> Send loading initial stats message for Champion ID: " + championId);
                lastFuture = playerChannel.writeAndFlush(championInitialStatsSend);
            } else {
                System.out.println(">>> [Log in MapHandler.handleChampionInitialStatsLoading] Champion with ID " + championId + " not found.");
            }
        }
        return lastFuture != null
            ? lastFuture
            : new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE).setSuccess(); 
    }
}
