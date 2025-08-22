package com.server.game.model.game.entityIface;

import com.server.game.model.game.context.CastSkillContext;

import lombok.extern.slf4j.Slf4j;

public interface SkillReceivable {

    void receiveSkillDamage(CastSkillContext ctx);
    Integer getDefense();

    default float calculateActualDamage(CastSkillContext ctx) {
        Float skillDamage = ctx.getSkillDamage();
        if (skillDamage == null || skillDamage < 0) {
            throw new IllegalArgumentException("Skill damage must be a non-negative value");
        }
        
        if (ctx.getTarget() == null) {
            throw new IllegalArgumentException("Target must not be null");
        }

        @SuppressWarnings("null")
        Integer myDefense = ctx.getTarget().getDefense();
        if (myDefense == null || myDefense <= 0) {
            throw new IllegalArgumentException("Defense must be a positive value");
        }

        System.out.println(String.format("Skill dmg=%f, defense=%d, actual dmg=%f", skillDamage, myDefense, skillDamage * (100.0f / (100 + myDefense))));
        return skillDamage * (100.0f / (100 + myDefense));
        // return 10f;
    }
}
