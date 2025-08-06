package com.server.game.model.game.component.attackComponent;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.context.CastSkillContext;

public abstract class SkillReceiver extends Entity {

    public SkillReceiver(String id, SlotState ownerSlot, GameState gameState) {
        super(id, ownerSlot, gameState);
    }

    public abstract void receiveSkillDamage(CastSkillContext ctx);

    protected final float calculateActualDamage(CastSkillContext ctx) {
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
        return skillDamage * (100.0f / (100 * myDefense));
    }
}
