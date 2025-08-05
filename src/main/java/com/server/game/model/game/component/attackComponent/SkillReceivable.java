package com.server.game.model.game.component.attackComponent;

import com.server.game.model.game.context.CastSkillContext;

public interface SkillReceivable {
    void receiveSkillDamage(CastSkillContext ctx);

    default float calculateActualDamage(CastSkillContext ctx) {
        Float casterDamage = ctx.getCasterDamage();
        if (casterDamage == null || casterDamage < 0) {
            throw new IllegalArgumentException("Caster damage must be a non-negative value");
        }
        
        if (ctx.getTarget() == null) {
            throw new IllegalArgumentException("Target must not be null");
        }

        @SuppressWarnings("null")
        Integer myDefense = ctx.getTarget().getDefense();
        if (myDefense == null || myDefense <= 0) {
            throw new IllegalArgumentException("Defense must be a positive value");
        }
        return casterDamage * (100.0f / (100 * myDefense));
    }
}
