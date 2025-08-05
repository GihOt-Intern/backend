package com.server.game.netty.messageHandler;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.factory.MoveContextFactory;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.MoveContext;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.PositionReceive;
import com.server.game.service.gameState.GameStateService;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class MoveMessageHandler {
    
    private final MoveContextFactory moveContextFactory;

    private GameStateService gameStateService;
    
    @MessageMapping(PositionReceive.class)
    public void handleMoveMessage(PositionReceive receiveObject, Channel channel) {
        
        String gameId = ChannelManager.getGameIdByChannel(channel);
        GameState gameState = gameStateService.getGameStateById(gameId);
        
        Entity mover = gameStateService.getEntityByStringId(gameState, receiveObject.getStringId());
        
        MoveContext ctx = moveContextFactory.createMoveContext(
            gameState,
            mover,
            receiveObject.getPosition(),
            receiveObject.getTimestamp()
        );

        mover.setMoveContext(ctx);
    }
    
} 