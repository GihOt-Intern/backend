package com.server.game.model.game.attackStrategy;

import org.springframework.stereotype.Component;

import com.server.game.model.game.context.AttackContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ChampionAttackStrategy implements AttackStrategy {

    @Override
    public boolean performAttack(AttackContext ctx) {

        // 1. First send attack animation of the attacker
        ctx.getGameStateService().sendAttackAnimation(ctx);

        // 2. Then perform the attack logic
        return ctx.getTarget().receiveAttack(ctx);
    }
}


