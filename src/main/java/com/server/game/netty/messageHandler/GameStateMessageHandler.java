package com.server.game.netty.messageHandler;


import org.springframework.stereotype.Component;

import com.server.game.model.game.Entity;
import com.server.game.netty.ChannelManager;
import com.server.game.model.game.GameState;
import com.server.game.netty.sendObject.PositionSend;
import com.server.game.netty.sendObject.attack.HealthUpdateSend;
import com.server.game.netty.sendObject.entity.EntityDeathSend;

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
        } catch (Exception e) {
            log.error("Exception in broadcastPositionUpdate: " + e.getMessage());
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
        } catch (Exception e) {
            log.error("Exception in broadcastHealthUpdate: " + e.getMessage());
        }
    }

    public void sendEntityDeath(String gameId, String entityId) {
        try {
            // Create health update message
            EntityDeathSend entityDeathSend = new EntityDeathSend(entityId);

            // Get any channel from the game to broadcast the health update
            Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
            channel.writeAndFlush(entityDeathSend);
        } catch (Exception e) {
            log.error("Exception in broadcastEntityDeath: " + e.getMessage());
        }
    }
}
