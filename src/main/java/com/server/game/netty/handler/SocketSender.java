package com.server.game.netty.handler;


import org.springframework.stereotype.Component;

import com.server.game.model.game.component.attackComponent.AttackContext;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.pvp.AttackAnimationDisplaySend;
import com.server.game.netty.sendObject.pvp.HealthUpdateSend;
import com.server.game.util.ChampionAnimationEnum;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class SocketSender {

    public static void sendAttackAnimation(AttackContext ctx, ChampionAnimationEnum animationEnum) {
        try {
            // Create attack animation display message
            AttackAnimationDisplaySend attackAnimation = new AttackAnimationDisplaySend(
                ctx.getAttackerSlot(), 
                ctx.getAttackerId(), 
                ctx.getTargetSlot(), 
                ctx.getTargetId(),
                animationEnum, 
                ctx.getAttacker().getAttackSpeed(),
                ctx.getTimestamp()
            );

            // Get any channel from the game to trigger the framework
            Channel channel = ChannelManager.getAnyChannelByGameId(ctx.getGameId());
            channel.writeAndFlush(attackAnimation);
            System.out.println("[Log in SocketSender#sendAttackAnimation] Sent AttackAnimationDisplaySend: " + attackAnimation);
        } catch (Exception e) {
            System.err.println("[Log in SocketSender#sendAttackAnimation] Exception in broadcastAttackerAnimation: " + e.getMessage());
        }
    }

    public static void sendHealthUpdate(AttackContext ctx, int actualDamage) {
        try {
            // Create health update message
            HealthUpdateSend healthUpdateSend = new HealthUpdateSend(
                ctx.getTargetSlot(),
                ctx.getTarget().getCurrentHP(),
                ctx.getTarget().getMaxHP(),
                actualDamage,
                ctx.getTimestamp()
            );

            // Get any channel from the game to trigger the framework
            Channel channel = ChannelManager.getAnyChannelByGameId(ctx.getGameId());
            channel.writeAndFlush(healthUpdateSend);
            System.out.println("[Log in SocketSender#sendHealthUpdate] Sent HealthUpdateSend: " + healthUpdateSend);
        } catch (Exception e) {
            System.err.println("[Log in SocketSender#sendHealthUpdate] Exception in broadcastHealthUpdate: " + e.getMessage());
        }
    }
}
