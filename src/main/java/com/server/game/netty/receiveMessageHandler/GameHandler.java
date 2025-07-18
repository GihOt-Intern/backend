package com.server.game.netty.receiveMessageHandler;


import java.util.List;

import org.springframework.stereotype.Component;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend.InitialPositionData;
import com.server.game.service.GameScheduler;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameHandler {

    MapHandler mapHandler;
    GameScheduler gameScheduler;
    
    public void handleGameStart(Channel channel) {
        
        List<InitialPositionData> initialPositionDatas = mapHandler.handleInitialGameStateLoading(channel);

        String gameId = ChannelManager.getGameIdByChannel(channel);
        gameScheduler.registerGame(gameId);


        for(InitialPositionData initialPositionsData : initialPositionDatas) {
            gameScheduler.updatePosition(
                gameId, 
                initialPositionsData.getSlot(), 
                initialPositionsData.getPosition(),
                System.currentTimeMillis()
            );
        }
    }
}