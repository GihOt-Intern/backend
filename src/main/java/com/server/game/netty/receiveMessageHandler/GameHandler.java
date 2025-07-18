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
import com.server.game.service.GameScheduler;
import com.server.game.service.PositionBroadcastService;
import com.server.game.service.PositionService;
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
