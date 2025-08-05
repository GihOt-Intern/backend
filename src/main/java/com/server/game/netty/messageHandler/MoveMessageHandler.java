package com.server.game.netty.messageHandler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.factory.MoveContextFactory;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.game.TroopInstance2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.PositionReceive;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.move.MoveService2;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class MoveMessageHandler {
    
    private final MoveContextFactory moveContextFactory;

    private final GameStateService gameStateService;
    private final MoveService2 moveService;

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

        moveService.setMove(ctx, true);
        
    }
    
} 