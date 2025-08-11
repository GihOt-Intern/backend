package com.server.game.model.game;

import com.server.game.model.game.context.CastSkillContext;

public abstract class SkillReceiverEntity extends DependentEntity {

    public SkillReceiverEntity(String id, SlotState ownerSlot, GameState gameState) {
        super(id, gameState, ownerSlot);
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
        // return skillDamage * (100.0f / (100 * myDefense));
        return 10f;
    }
}
