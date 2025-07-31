package com.server.game.model.game.component.attackComponent;


import com.server.game.model.game.Entity;
import com.server.game.util.Util;

import lombok.Getter;

@Getter
public class AttackComponent {
    private Entity owner;
    private int damage;
    private float attackSpeed;
    private int attackDelayTick;
    private long nextAttackTick;

    private AttackStrategy strategy;


    public AttackComponent(Entity owner, int damage, float attackSpeed, AttackStrategy strategy) {
        this.owner = owner;
        this.strategy = strategy;
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.attackDelayTick = (int) attackSpeed * Util.getGameTickIntervalMs();
        this.nextAttackTick = 0;
    }

    
    public float getAttackSpeed() {
        return attackSpeed;
    }

    public int getDamage() {
        return damage;
    }


    public final boolean canAttack(long currentTick) {
        return currentTick >= this.nextAttackTick;
    }

    public final void performAttack(AttackContext ctx) {
        long currentTick = ctx.getCurrentTick();

        // if (ctx.getTarget() == null) {
        //     throw new IllegalArgumentException("Target cannot be null");
        // }

        // if (!this.canAttack(currentTick)) { return; }

        System.out.println(">>> [Log in AttackComponent] Performing attack with strategy: " + strategy.getClass().getSimpleName());
        // Use the strategy to perform the attack
        strategy.performAttack(ctx);

        // After performing the attack, update the next attack tick
        this.nextAttackTick = currentTick + attackDelayTick;
    }
}