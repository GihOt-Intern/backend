package com.server.game.model.game.component.attackComponent;


import com.server.game.model.game.Entity;
import com.server.game.model.game.attackStrategy.AttackStrategy;
import com.server.game.model.game.context.AttackContext;
import com.server.game.util.Util;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.move.MoveService2;

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

    private final MoveService2 moveService;


    public AttackComponent(Entity owner, int damage, float attackSpeed, float attackRange, 
        AttackStrategy strategy, MoveService2 moveService) {

        this.owner = owner;
        this.strategy = strategy;
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.attackRange = attackRange;
        this.attackDelayTick = Math.round(1000.0f / (attackSpeed * Util.getGameTickIntervalMs()));
        this.nextAttackTick = 0;
    
        this.moveService = moveService;
    }

    public void setAttackContext(AttackContext ctx) {
        this.attackContext = ctx;
        System.out.println(">>> [Log in AttackComponent] Attack context set: " + ctx);
    }

    public boolean isAttacking() {
        return this.attackContext != null && this.attackContext.getTarget() != null;
    }

    private final boolean inAttackWindow(long currentTick) {
        return currentTick >= this.nextAttackTick;
    }

    private final boolean inAttackRange(Vector2 targetPosition) {
        float distance = this.owner.getCurrentPosition().distance(targetPosition);
        System.out.println(">>> [Log in AttackComponent] Checking attack range: " + 
            "distance=" + distance + ", attackRange=" + this.attackRange);
        return distance - 0.1f <= this.attackRange;
    }

    public final boolean inAttackRange() {
        if (this.attackContext == null || this.attackContext.getTarget() == null) {
            System.out.println(">>> [Log in AttackComponent] Attack context or target is null, cannot check attack range.");
            return false;
        }
        return inAttackRange(this.attackContext.getTarget().getCurrentPosition());
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

        if (!this.inAttackWindow(currentTick)) {  
            System.out.println(">>> [Log in AttackComponent] Not in attack window, current tick: " + currentTick + ", next attack tick: " + this.nextAttackTick);
            return false;  
        }


        if (!this.inAttackRange(ctx.getTarget().getCurrentPosition())) {
            System.out.println(">>> [Log in AttackComponent] Target is out of attack range, trying to stick to target");
            System.out.println(">>> Current position: " + this.owner.getCurrentPosition() + 
                ", Target position: " + ctx.getTarget().getCurrentPosition() + 
                ", Attack range: " + this.attackRange);

            Vector2 ownerPosition = this.owner.getCurrentPosition();
            Vector2 targetPosition = ctx.getTarget().getCurrentPosition();
            
            Vector2 direction = targetPosition.subtract(ownerPosition).normalize();
            Vector2 newPosition = ownerPosition.add(direction.multiply(this.attackRange));

            moveService.setMove(this.owner, newPosition, false);
            // moveService.setMove(this.owner, ctx.getTarget().getCurrentPosition(), false);
            return false;
        }

        System.out.println(">>> [Log in AttackComponent] Performing attack with strategy: " + 
            strategy.getClass().getSimpleName());

        // Stop moving before performing the attack
        moveService.setStopMoving(this.owner);
        System.out.println(">>> [Log in AttackComponent] Stopped moving before attack");
        
        // Use the strategy to perform the attack
        boolean didAttack = strategy.performAttack(ctx);

        if (ctx.getTarget() == null || !ctx.getTarget().isAlive()) {
            System.out.println(">>> [Log in AttackComponent] After performing attack, target is null or dead");
            moveService.setStopMoving(this.owner);
            this.attackContext = null; // Clear the attack context if target is dead
        }

        // After performing the attack, update the next attack tick
        this.nextAttackTick = currentTick + attackDelayTick;

        return didAttack;
    }
}