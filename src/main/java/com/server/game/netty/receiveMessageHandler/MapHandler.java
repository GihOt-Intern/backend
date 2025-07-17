package com.server.game.netty.receiveMessageHandler;


import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend.InitialPositionData;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend;
import com.server.game.resource.service.GameMapService;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MapHandler {

    GameMapService gameMapService;

    // This method is called by LobbyHandler when all players are ready
    public void handleInitialGameStateLoading(Channel channel) {
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        Short numPlayers = (short) playersInRoom.size();

        // GameMap id is determined by the number of players
        Short gameMapId = numPlayers;

        List<InitialPositionData> initialPositionsData = 
            gameMapService.getInitialPositionsData(gameMapId);

        InitialPositionsSend championPositionsSend = 
            new InitialPositionsSend(gameMapId, initialPositionsData);
        System.out.println(">>> Send loading map messsage");
        channel.writeAndFlush(championPositionsSend);        
    }

}
