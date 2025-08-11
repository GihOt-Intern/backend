package com.server.game.model.game.component.attackComponent;


import org.springframework.lang.Nullable;

import com.server.game.model.game.Building;
import com.server.game.model.game.Entity;
import com.server.game.model.game.attackStrategy.AttackStrategy;
import com.server.game.model.game.context.AttackContext;
import com.server.game.util.Util;
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

    public boolean setAttackContext(@Nullable AttackContext ctx) {

        if (ctx == null) { // ctx null means forced stop attack
            // System.out.println(">>> [Log in AttackComponent] Setting attack context to null, force stopping attack.");
            this.attackContext = null;
            return true;
        }
        
        if (this.owner.isCastingDurationSkill() && !this.owner.canUseSkillWhileAttacking()) {
            // System.out.println(">>> [Log in AttackComponent] Cannot set attack context while casting skill, skipping.");
            return false;
        }

        this.attackContext = ctx;
        // System.out.println(">>> [Log in AttackComponent] Attack context set: " + ctx);
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

        


        if (!this.inAttackRange() && !this.owner.getStringId().startsWith("tower")) {
            // System.out.println(">>> [Log in AttackComponent] Target is out of attack range, trying to move to position that reach attack range.");
            // System.out.println(">>> Current position: " + this.owner.getCurrentPosition() + 
            //     ", Target position: " + ctx.getTarget().getCurrentPosition() + 
            //     ", Attack range: " + this.attackRange);

            // Vector2 ownerPosition = this.owner.getCurrentPosition();
            // Vector2 targetPosition = ctx.getTarget().getCurrentPosition();
            
            // Vector2 direction = targetPosition.subtract(ownerPosition).normalize();
            // Vector2 newPosition = ownerPosition.add(
            //     direction.multiply(owner.getDistanceNeededToReachAttackRange()));

            // moveService.setMove(this.owner, newPosition, false);

            moveService.setMove(this.owner, ctx.getTarget().getCurrentPosition(), false);
            return false;
        }

        if (!this.inAttackWindow(currentTick)) {  
            // System.out.println(">>> [Log in AttackComponent] Not in attack window, current tick: " + currentTick + ", next attack tick: " + this.nextAttackTick);
            return false;  
        }


        // Stop moving before performing the attack
        if (!this.owner.getStringId().startsWith("tower")) {
            moveService.setStopMoving(this.owner, false);
        }
            // System.out.println(">>> [Log in AttackComponent] Stopped moving before attack");
        
        // Use the strategy to perform the attack
        // short attakerSlot = this.owner.getOwnerSlot().getSlot();
        // short targetSlot = ctx.getTarget().getOwnerSlot().getSlot();
        // if (attakerSlot == targetSlot) {
        //     return false; // Cannot attack allies
        // }
        boolean didAttack = strategy.performAttack(ctx);

        if (ctx.getTarget() == null || !ctx.getTarget().isAlive()) {
            System.out.println(">>> [Log in AttackComponent] After performing attack, target is null or dead");
            moveService.setStopMoving(this.owner, true);
            this.stopAttacking();
        }

        // After performing the attack, update the next attack tick
        this.nextAttackTick = currentTick + attackDelayTick;

        return didAttack;
    }
}