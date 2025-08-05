package com.server.game.model.game.component.attackComponent;


import com.server.game.config.SpringContextHolder;
import com.server.game.model.game.Entity;
import com.server.game.model.game.attackStrategy.AttackStrategy;
import com.server.game.model.game.context.AttackContext;
import com.server.game.util.Util;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.attack.AttackService;

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
        this.attackDelayTick = Math.round(1000.0f / (attackSpeed * Util.getGameTickIntervalMs()));
        this.nextAttackTick = 0;
    }

    public void setAttackContext(AttackContext ctx) {
        this.attackContext = ctx;
        System.out.println(">>> [Log in AttackComponent] Attack context set: " + ctx);
    }

    private final boolean inAttackWindow(long currentTick) {
        return currentTick >= this.nextAttackTick;
    }

    private final boolean inAttackRange(Vector2 targetPosition) {
        float distance = this.owner.getCurrentPosition().distance(targetPosition);
        System.out.println(">>> [Log in AttackComponent] Checking attack range: " + 
            "distance=" + distance + ", attackRange=" + this.attackRange);
        return distance-1 <= this.attackRange;
        // GridCell ownerCell = this.getAttackContext().getGameStateService()
        //     .getGridCellByEntity(this.getAttackContext().getGameState(), this.owner);
        // GridCell targetCell = this.getAttackContext().getGameStateService()
        //     .getGridCellByEntity(this.getAttackContext().getGameState(), 
        //         this.getAttackContext().getTarget());
        // if (ownerCell == null || targetCell == null) {
        //     System.out.println(">>> [Log in AttackComponent] Owner or target cell is null, returning false");
        //     return false;
        // }
        // System.out.println(">>> [Log in AttackComponent] Checking attack range: " + 
        //     "ownerCell=" + ownerCell + ", targetCell=" + targetCell + 
        //     ", attackRange=" + this.attackRange);
        // return false;
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


        if (!this.inAttackRange(ctx.getTarget().getCurrentPosition())) {
            owner.setMove2Target(ctx.getTarget());
            return false;
        }

        if (!this.inAttackWindow(currentTick)) {  
            return false;  
        }

        // Stop moving before performing the attack
        owner.setStopMoving();
        
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