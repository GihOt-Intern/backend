package com.server.game.service.defense;

import com.server.game.factory.AttackContextFactory;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.service.attack.AttackService;
import com.server.game.service.gameState.GameStateService;
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
            .forEach(troop -> processTroopDefense(troop, gameState));
    }

    private void processTroopDefense(TroopInstance2 troop, GameState gameState) {
        // Skip if not in defensive stance (e.g., has a manual attack/move order)
        if (!troop.isInDefensiveStance() || troop.isAttacking()) {
            return;
        }

        // --- Handle existing target ---
        if (troop.getDefensiveTarget() != null) {
            Entity target = troop.getDefensiveTarget();
            // If target is dead or out of defense range, disengage
            if (!target.isAlive() || !troop.isWithinDefenseRange(target)) {
                troop.setDefensiveTarget(null);
                moveService.setMove(troop, troop.getDefensePosition(), true); // Return to base
                log.trace("Troop {} disengaging, target left defense range. Returning to {}.", troop.getStringId(), troop.getDefensePosition());
            } else {
                // Target is valid, set attack context to chase and attack
                attackService.setAttack(attackContextFactory.createAttackContext(
                    gameState.getGameId(), troop.getStringId(), target.getStringId(), gameState.getCurrentTick()
                ));
            }
            return;
        }

        // --- Find new target ---
        findNearestEnemyInDetectionRange(troop, gameState).ifPresent(enemy -> {
            log.trace("Troop {} detected new enemy {} in detection range.", troop.getStringId(), enemy.getStringId());
            troop.setDefensiveTarget(enemy);
            // The logic will pick up the attack on the next tick
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