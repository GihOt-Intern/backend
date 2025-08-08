package com.server.game.service.defense;

import com.server.game.factory.AttackContextFactory;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.service.attack.AttackService;
import com.server.game.service.move.MoveService2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefensiveStanceService {
    private final MoveService2 moveService;
    private final AttackService attackService;
    private final AttackContextFactory attackContextFactory;

    public void updateDefensiveStances(GameState gameState) {
        gameState.getEntities().stream()
            .filter(e -> e instanceof TroopInstance2)
            .map(e -> (TroopInstance2) e)
            .forEach(troop -> {
                // First check if troop should re-enable defensive stance
                troop.checkAndEnableDefensiveStance();
                // Then process defense logic
                processTroopDefense(troop, gameState);
            });
    }

    private void processTroopDefense(TroopInstance2 troop, GameState gameState) {
        // Skip if not in defensive stance, but allow processing if attacking defensively
        if (!troop.isInDefensiveStance()) {
            return;
        }

        // --- Check if troop is outside defense range and needs to return ---
        if (!troop.isWithinOwnDefenseRange()) {
            // If troop is outside defense range, clear target and return to base
            if (troop.getDefensiveTarget() != null) {
                log.debug("Troop {} is outside defense range, clearing target and returning to base", troop.getStringId());
                troop.setDefensiveTarget(null);
                // Clear any attack context when returning to base
                troop.getAttackComponent().setAttackContext(null);
            }
            // Force return to defense position
            moveService.setMove(troop, troop.getDefensePosition(), true);
            log.trace("Troop {} returning to defense position {} (outside defense range)", 
                troop.getStringId(), troop.getDefensePosition());
            return;
        }

        // --- Handle existing target ---
        if (troop.getDefensiveTarget() != null) {
            Entity target = troop.getDefensiveTarget();
            // Use detection range for consistency, not defense range
            boolean targetInRange = troop.getCurrentPosition().distance(target.getCurrentPosition()) <= troop.getDetectionRange();
            
            if (!target.isAlive() || !targetInRange) {
                troop.setDefensiveTarget(null);
                troop.getAttackComponent().setAttackContext(null); // Clear attack
                moveService.setMove(troop, troop.getDefensePosition(), true);
                log.trace("Troop {} disengaging, target left detection range. Returning to {}.", troop.getStringId(), troop.getDefensePosition());
            } else {
                // Target is valid, continue attacking (don't exit early for attacking state)
                if (!troop.isAttacking()) {
                    attackService.setAttack(attackContextFactory.createAttackContext(
                        gameState.getGameId(), troop.getStringId(), target.getStringId(), gameState.getCurrentTick()
                    ));
                }
            }
            return;
        }

        // --- Find new target ---
        findNearestEnemyInDetectionRange(troop, gameState).ifPresent(enemy -> {
            log.trace("Troop {} detected new enemy {} in detection range.", troop.getStringId(), enemy.getStringId());
            troop.setDefensiveTarget(enemy);
            // Immediately start attacking the new target
            attackService.setAttack(attackContextFactory.createAttackContext(
                gameState.getGameId(), troop.getStringId(), enemy.getStringId(), gameState.getCurrentTick()
            ));
        });

        // --- Return to post if idle and away ---
        if (troop.getDefensiveTarget() == null && !troop.isMoving()) {
            if (troop.getCurrentPosition().distance(troop.getDefensePosition()) > 0.5f) {
                log.trace("Troop {} is idle and away from post. Returning to {}.", troop.getStringId(), troop.getDefensePosition());
                moveService.setMove(troop, troop.getDefensePosition(), true);
            }
        }
    }

    private Optional<Entity> findNearestEnemyInDetectionRange(TroopInstance2 troop, GameState gameState) {
        return gameState.getEntities().stream()
            .filter(Entity::isAlive)
            .filter(e -> e.getOwnerSlot().getSlot() != troop.getOwnerSlot().getSlot()) // Is an enemy
            .filter(e -> troop.getCurrentPosition().distance(e.getCurrentPosition()) <= troop.getDetectionRange())
            .min(Comparator.comparing(e -> troop.getCurrentPosition().distance(e.getCurrentPosition())));
    }
}