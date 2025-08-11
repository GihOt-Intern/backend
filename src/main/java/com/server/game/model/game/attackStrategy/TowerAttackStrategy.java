package com.server.game.model.game.attackStrategy;

import com.server.game.model.game.context.AttackContext;

public class TowerAttackStrategy implements AttackStrategy {
    @Override
    public boolean performAttack(AttackContext ctx) {

        // 1. First send attack animation of the attacker
        ctx.getGameStateService().sendAttackAnimation(ctx);

        return ctx.getTarget().receiveAttack(ctx);
    }
}