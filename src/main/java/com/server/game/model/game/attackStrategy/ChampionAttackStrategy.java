package com.server.game.model.game.attackStrategy;

import org.springframework.stereotype.Component;

import com.server.game.model.game.context.AttackContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ChampionAttackStrategy implements AttackStrategy {


    @Override
    public void performAttack(AttackContext ctx) {

        // 1. First send attack animation of the attacker
        System.out.println(">>> [Log in ChampionAttackStrategy] Sending attack animation for attacker");
        // AnimationSender.sendAttackAnimation(ctx, ChampionAnimationEnum.ATTACK_ANIMATION);
    
    
        // 2. Then perform the attack logic
        System.out.println(">>> [Log in ChampionAttackStrategy] Performing attack logic");
        ctx.getTarget().receiveAttack(ctx);
    }
}


