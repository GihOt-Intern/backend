package com.server.game.netty.messageHandler;


import org.springframework.stereotype.Component;

import com.server.game.model.game.Entity;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.CastSkillSend;
import com.server.game.netty.sendObject.attack.HealthUpdateSend;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class GameStateMessageHandler {

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
