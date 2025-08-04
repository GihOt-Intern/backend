package com.server.game.model.game.component.attackComponent;


import com.server.game.model.game.Entity;
import com.server.game.model.game.attackStrategy.AttackStrategy;
import com.server.game.model.game.context.AttackContext;
import com.server.game.util.Util;
import com.server.game.model.map.component.Vector2;

import lombok.Getter;

@Getter
public class AttackComponent {
    private Entity owner;
    private int damage;
    private float attackSpeed;
    private float attackRange;
    private int attackDelayTick;
    private long nextAttackTick;

    private AttackContext attackContext = null;

    private final AttackStrategy strategy;


    public AttackComponent(Entity owner, int damage, float attackSpeed, float attackRange, AttackStrategy strategy) {
        this.owner = owner;
        this.strategy = strategy;
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.attackRange = attackRange;
        this.attackDelayTick = (int) attackSpeed * Util.getGameTickIntervalMs();
        this.nextAttackTick = 0;
    }

    public void setAttackContext(AttackContext ctx) {
        this.attackContext = ctx;
    }

    private final boolean inAttackWindow(long currentTick) {
        return currentTick >= this.nextAttackTick;
    }

    private final boolean inAttackRange(Vector2 targetPosition) {
        float distance = this.owner.getCurrentPosition().distance(targetPosition);
        return distance <= this.attackRange;
    }


    public final boolean performAttack() {
        AttackContext ctx = this.attackContext;
        if (ctx == null) {
            System.out.println(">>> [Log in AttackComponent] No attack context set, nothing to perform attack");
            return false;
        }


        long currentTick = ctx.getCurrentTick();

        if (ctx.getTarget() == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }

        if (!this.inAttackWindow(currentTick)) {  return false;  }

        if (!this.inAttackRange(ctx.getTarget().getCurrentPosition())) {
            return false;
        }


        System.out.println(">>> [Log in AttackComponent] Performing attack with strategy: " + 
            strategy.getClass().getSimpleName());

        // Use the strategy to perform the attack
        boolean didAttack = strategy.performAttack(ctx);

        if (ctx.getTarget() == null || !ctx.getTarget().isAlive()) {
            System.out.println(">>> [Log in AttackComponent] After performing attack, target is null or dead");
            this.setAttackContext(null);
        }

        // After performing the attack, update the next attack tick
        this.nextAttackTick = currentTick + attackDelayTick;

        return didAttack;
    }
}