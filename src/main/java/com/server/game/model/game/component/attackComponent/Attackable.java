package com.server.game.model.game.component.attackComponent;

import com.server.game.model.game.context.AttackContext;

public interface Attackable {
    boolean receiveAttack(AttackContext ctx);

    default float calculateActualDamage(AttackContext ctx) {
        int attackerDamage = ctx.getAttacker().getDamage();
        int myDefense = ctx.getTarget().getDefense();
        // if (myDefense == 0) {
        //     throw new IllegalArgumentException("Defense=0, stupid!");
        // }
        //For testing purposes, we will not use the defense value
        return attackerDamage;
        // return 1f;
        // return attackerDamage * (100.0f / (100 * myDefense));
    }
}
