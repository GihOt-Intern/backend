package com.server.game.netty.messageHandler;


import org.springframework.stereotype.Component;

import com.server.game.model.game.context.AttackContext;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.pvp.HealthUpdateSend;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class GameStateMessageHandler {

    public void sendHealthUpdate(AttackContext ctx) {
        try {
            // Create health update message
            HealthUpdateSend healthUpdateSend = new HealthUpdateSend(
                ctx.getTarget().getStringId(),
                ctx.getTarget().getCurrentHP(),
                ctx.getTarget().getMaxHP(),
                ctx.getActualDamage(),
                ctx.getTimestamp()
            );

            // Get any channel from the game to broadcast the health update
            Channel channel = ChannelManager.getAnyChannelByGameId(ctx.getGameId());
            channel.writeAndFlush(healthUpdateSend);
            System.out.println("[Log in SocketSender#sendHealthUpdate] Sent HealthUpdateSend: " + healthUpdateSend);
        } catch (Exception e) {
            System.err.println("[Log in SocketSender#sendHealthUpdate] Exception in broadcastHealthUpdate: " + e.getMessage());
        }
    }
}
