package com.server.game.model.game.component.attackComponent;

import org.springframework.stereotype.Component;

import com.server.game.model.game.TroopInstance2;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.netty.handler.SocketSender;
import com.server.game.util.ChampionAnimationEnum;
import com.server.game.util.TroopEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TroopAttackStrategy implements AttackStrategy {

    @Override
    public void performAttack(AttackContext ctx) {
        if (ctx.getAttacker() == null || ctx.getTarget() == null) {
            log.warn("Cannot perform attack with null attacker or target");
            return;
        }

        // Cast entities to TroopInstance2 if they are troops
        if (!(ctx.getAttacker() instanceof com.server.game.model.game.TroopInstance2) ||
            !(ctx.getTarget() instanceof com.server.game.model.game.TroopInstance2)) {
            log.warn("TroopAttackStrategy can only handle TroopInstance2 attackers and targets");
            return;
        }

        com.server.game.model.game.TroopInstance2 attacker = (com.server.game.model.game.TroopInstance2) ctx.getAttacker();
        com.server.game.model.game.TroopInstance2 target = (com.server.game.model.game.TroopInstance2) ctx.getTarget();

        // 1. Determine if this is a healing action or damage action
        boolean isHealing = false;
        
        // Healing happens between friendly troops when attacker has healing power
        if (attacker.getTroopType() == com.server.game.util.TroopEnum.HEALER && 
            attacker.getOwnerSlot() == target.getOwnerSlot()) {
            isHealing = true;
        }
        
        // Send appropriate animation
        if (isHealing) {
            // Healing animation - use a different animation type
            SocketSender.sendAttackAnimation(ctx, ChampionAnimationEnum.ATTACK_ANIMATION); // Use HEAL when available
            
            // Apply healing
            int healAmount = attacker.getHealingPower();
            target.heal(healAmount);
            
            log.debug("Troop {} healed {} for {} HP", 
                    attacker.getIdAString(), target.getIdAString(), healAmount);
        } else {
            // Attack animation
            SocketSender.sendAttackAnimation(ctx, ChampionAnimationEnum.ATTACK_ANIMATION);
            
            // Calculate damage
            int attackerDamage = attacker.getAttackComponent().getDamage();
            int targetDefense = target.getDefense();
            
            // Apply damage formula
            float defenseMultiplier = target.isBuffed() ? target.getDefenseMultiplier() : 1.0f;
            int actualDamage = Math.max(1, (int) (attackerDamage * (100.0f / (100.0f + targetDefense * defenseMultiplier))));
            
            // Apply damage using health component
            target.getHealthComponent().takeDamage(actualDamage);
            
            log.debug("Troop {} attacked {} for {} damage (health now: {}/{})", 
                    attacker.getIdAString(), target.getIdAString(), actualDamage,
                    target.getCurrentHP(), target.getMaxHP());
                    
            // Update AI state if target died
            if (!target.isAlive()) {
                attacker.setCurrentTargetId(null);
                attacker.setAIState(com.server.game.model.game.TroopInstance2.TroopAIState.IDLE);
                log.info("Troop {} killed by {}", target.getIdAString(), attacker.getIdAString());
            }
        }
        
        // 2. Send health update to clients
        SocketSender.sendHealthUpdate(ctx, isHealing ? attacker.getHealingPower() : 
                                           attacker.getAttackComponent().getDamage());
        // System.out.println(">>> [Log in ChampionAttackStrategy] Performing attack logic");
        // ctx.getTarget().receiveAttack(ctx);
    }
}


