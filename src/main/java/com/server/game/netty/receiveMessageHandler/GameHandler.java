package com.server.game.netty.receiveMessageHandler;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.map.MapWorld;
import com.server.game.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.ChampionInitialHPsSend.ChampionInitialHPData;
import com.server.game.netty.messageObject.sendObject.ChampionInitialStatsSend;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend.InitialPositionData;
import com.server.game.resource.model.Champion;
import com.server.game.resource.service.GameMapService;
import com.server.game.service.GameScheduler;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameHandler {

    MapHandler mapHandler;
    GameScheduler gameScheduler;
    
    public void handleGameStart(Channel channel) {
        
        // List<InitialPositionData> initialPositionDatas = mapHandler.handleInitialGameStateLoading(channel);

        // Map<String, Object> initialGameState = mapHandler.handleInitialGameStateLoading(channel);

        // List<InitialPositionData> initialPositionDatas = 
        //     (List<InitialPositionData>) initialGameState.get("initialPositions");
        // List<ChampionInitialHPData> championInitialHPs = 
        //     (List<ChampionInitialHPData>) initialGameState.get("championInitialHPs");
        // List<ChampionInitialStatsSend> championInitialStatsSends = 
        //     (List<ChampionInitialStatsSend>) initialGameState.get("championInitialStats");


        // String gameId = ChannelManager.getGameIdByChannel(channel);




        // gameScheduler.registerGame(gameId);


        // for(InitialPositionData initialPositionsData : initialPositionDatas) {
        //     gameScheduler.updatePosition(
        //         gameId, 
        //         initialPositionsData.getSlot(), 
        //         initialPositionsData.getPosition(),
        //         5.0f,
        //         System.currentTimeMillis()
        //     );
        // }
    }


    
}