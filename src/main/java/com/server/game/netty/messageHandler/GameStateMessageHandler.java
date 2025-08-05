package com.server.game.netty.messageHandler;


import org.springframework.stereotype.Component;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.PositionSend;
import com.server.game.netty.sendObject.attack.HealthUpdateSend;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class GameStateMessageHandler {

    public void sendPositionUpdate(GameState gameState, Entity mover) {
        try {
            // Create position update message
            PositionSend positionSend = new PositionSend(
                mover.getStringId(),
                mover.getCurrentPosition(),
                mover.getMoveSpeed(),
                System.currentTimeMillis()
            );

            // Get any channel from the game to broadcast the position update
            Channel channel = ChannelManager.getAnyChannelByGameId(gameState.getGameId());
            channel.writeAndFlush(positionSend);
            System.out.println("[Log in SocketSender#sendPositionUpdate] Sent PositionSend: " + positionSend);
        } catch (Exception e) {
            System.err.println("[Log in SocketSender#sendPositionUpdate] Exception in broadcastPositionUpdate: " + e.getMessage());
        }
    }

    public void sendHealthUpdate(String gameId, Entity target, int actualDamage, long timestamp) {
        try {
            // Create health update message
            HealthUpdateSend healthUpdateSend = new HealthUpdateSend(
                target.getStringId(),
                target.getCurrentHP(),
                target.getMaxHP(),
                actualDamage,
                timestamp
            );

            // Get any channel from the game to broadcast the health update
            Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
            channel.writeAndFlush(healthUpdateSend);
            System.out.println("[Log in SocketSender#sendHealthUpdate] Sent HealthUpdateSend: " + healthUpdateSend);
        } catch (Exception e) {
            System.err.println("[Log in SocketSender#sendHealthUpdate] Exception in broadcastHealthUpdate: " + e.getMessage());
        }
    }
}
