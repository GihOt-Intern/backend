package com.server.game.model.game.attackStrategy;

import org.springframework.stereotype.Component;

import com.server.game.model.game.context.AttackContext;
import com.server.game.netty.messageHandler.AnimationMessageHandler;
import com.server.game.util.ChampionAnimationEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TroopAttackStrategy implements AttackStrategy {

    @Override
    public void performAttack(AttackContext ctx) {

        // TODO: please migrate handling troop attack any Entity to this method


        // // 1. First send attack animation of the attacker
        // System.out.println(">>> [Log in ChampionAttackStrategy] Sending attack animation for attacker");
        // SocketSender.sendAttackAnimation(ctx, AnimationEnum.ATTACK_ANIMATION);
    
    
        // // 2. Then perform the attack logic
        // System.out.println(">>> [Log in ChampionAttackStrategy] Performing attack logic");
        // ctx.getTarget().receiveAttack(ctx);
    }
}


