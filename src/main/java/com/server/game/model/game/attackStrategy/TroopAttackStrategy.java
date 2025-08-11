package com.server.game.model.game.attackStrategy;

import org.springframework.stereotype.Component;

import com.server.game.model.game.context.AttackContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TroopAttackStrategy implements AttackStrategy {

    @Override
    public boolean performAttack(AttackContext ctx) {
        // target is a independent entity, like gold mine
        if (ctx.getTarget().getOwnerSlot() == null) {
            return false; // troop does not attack independent entities
        }

    
        if (ctx.getAttacker().isAllies(ctx.getTarget())) {
            return false; // Cannot attack allies
        }

        // 1. First send attack animation of the attacker
        ctx.getGameStateService().sendAttackAnimation(ctx);

        // 2. Then perform the attack logic
        return ctx.getTarget().receiveAttack(ctx);
    }
}


