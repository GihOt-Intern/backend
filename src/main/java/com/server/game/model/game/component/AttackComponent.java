package com.server.game.model.game.component;


import org.springframework.lang.Nullable;

import com.server.game.model.game.Entity;
import com.server.game.model.game.attackStrategy.AttackStrategy;
import com.server.game.model.game.context.AttackContext;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttackComponent {
    Entity owner;
    int damage;
    float attackSpeed;
    float attackRange;
    int attackDelayTick;
    long nextAttackTick;

    final AttackStrategy strategy;

    AttackContext attackContext = null;


    public AttackComponent(Entity owner, int damage, float attackSpeed, float attackRange, 
        AttackStrategy strategy) {

        this.owner = owner;
        this.strategy = strategy;
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.attackRange = attackRange;
        this.attackDelayTick = Math.round(1000.0f / (attackSpeed * Util.getGameTickIntervalMs()));
        this.nextAttackTick = 0;
    }

    public boolean setAttackContext(@Nullable AttackContext ctx) {

        if (ctx == null) { // ctx null means forced stop attack
            this.attackContext = null;
            return true;
        }
        
        if (this.owner.isCastingDurationSkill() && !this.owner.canPerformSkillWhileAttacking()) {
            return false;
        }

        this.attackContext = ctx;
        return true;
    }

    public Entity getAttackTarget() {
        if (this.attackContext != null) {
            return this.attackContext.getTarget();
        }
        return null;
    }

    public boolean isAttacking() {
        return this.attackContext != null && this.attackContext.getTarget() != null;
    }
    
    public void stopAttacking() {
        this.setAttackContext(null);
    }

    private final boolean inAttackWindow(long currentTick) {
        return currentTick >= this.nextAttackTick;
    }

    private final boolean inAttackRange(Entity target) {
        // .distanceTo(Entity) method in Entity has handled distance calculating
        // of all instances cases of subclasses
        Float distance = this.owner.distanceTo(target);
        return distance - 0.1f <= this.attackRange;
    }

    public final boolean inAttackRange() {
        if (this.attackContext == null || this.attackContext.getTarget() == null) {
            return false;
        }
        return inAttackRange(this.attackContext.getTarget());
    }


    public final boolean performAttack() {
        AttackContext ctx = this.attackContext;
        if (ctx == null) {
            return false;
        }

        long currentTick = ctx.getCurrentTick();

        if (ctx.getTarget() == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }

        
        if (!this.inAttackRange()) {
            owner.getGameStateService().setMove(
                this.owner, ctx.getTarget().getCurrentPosition(), false);
            return false;
        }

        if (!this.inAttackWindow(currentTick)) {  
            return false;  
        }


        // Stop moving before performing the attack
        owner.getGameStateService().setStopMoving(this.owner, false);

        // Use the strategy to perform the attack
        // .performAttack() has handled to do not attack allies
        boolean didAttack = strategy.performAttack(ctx);

        if (ctx.getTarget() == null || !ctx.getTarget().isAlive()) {
            log.info("After performing attack, target is null or dead");
            owner.getGameStateService().setStopMoving(this.owner, true);
            this.stopAttacking();
        }

        // After performing the attack, update the next attack tick
        this.nextAttackTick = currentTick + attackDelayTick;

        return didAttack;
    }
}