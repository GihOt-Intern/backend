package com.server.game.netty.messageHandler;


import org.springframework.stereotype.Component;

import com.server.game.model.game.context.AttackContext;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.attack.AttackAnimationSend;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class AnimationMessageHandler {

    public void sendAttackAnimation(AttackContext ctx) {
        try {
            // Create attack animation display message
            AttackAnimationSend attackAnimation = new AttackAnimationSend(ctx);

            // Get any channel from the game to broadcast the animation
            Channel channel = ChannelManager.getAnyChannelByGameId(ctx.getGameId());
            channel.writeAndFlush(attackAnimation);
            System.out.println("[Log in SocketSender#sendAttackAnimation] Sent AttackAnimationDisplaySend: " + attackAnimation);
        } catch (Exception e) {
            System.err.println("[Log in SocketSender#sendAttackAnimation] Exception in broadcastAttackerAnimation: " + e.getMessage());
        }
    }
}
