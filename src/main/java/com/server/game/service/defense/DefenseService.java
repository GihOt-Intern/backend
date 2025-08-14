package com.server.game.service.defense;

import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.server.game.factory.AttackContextFactory;
import com.server.game.model.game.Champion;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.Troop;
import com.server.game.model.game.building.Burg;
import com.server.game.model.game.building.Tower;
import com.server.game.service.attack.AttackService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for handling the defense mechanisms of buildings like Towers and Burgs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefenseService {
    private final AttackService attackService;
    private final AttackContextFactory attackContextFactory;

    /**
     * Update the defense for all defensive structures (Towers and Burgs)
     */
    public void updateDefenses(GameState gameState) {
        // Process tower defenses
        gameState.getEntities().stream()
            .filter(entity -> entity instanceof Tower)
            .map(entity -> (Tower) entity)
            .forEach(tower -> processTowerDefense(tower, gameState));
        
        // Process burg defenses
        gameState.getEntities().stream()
            .filter(entity -> entity instanceof Burg)
            .map(entity -> (Burg) entity)
            .forEach(burg -> processBurgDefense(burg, gameState));
    }

    /**
     * Process defense mechanism for a tower
     */
    private void processTowerDefense(Tower tower, GameState gameState) {
        if (!tower.isAlive()) {
            return;
        }

        if (tower.isAttacking()) {
            Entity currentTarget = tower.getAttackComponent().getAttackContext().getTarget();
            boolean targetInRange = tower.getAttackComponent().inAttackRange();

            if (!currentTarget.isAlive() || !targetInRange) {
                tower.getAttackComponent().setAttackContext(null);
                log.trace("Tower {} target lost, searching for new target...", tower.getStringId());
            } else {
                return;
            }
        }

        // --Find new target--
        findPriorityTargetInRange(tower, gameState).ifPresent(enemy -> {
            log.trace("Tower {} found new target: {}", tower.getStringId(), enemy.getStringId());

            attackService.setAttack(attackContextFactory.createAttackContext(
                gameState.getGameId(), tower.getStringId(), enemy.getStringId(), gameState.getCurrentTick()
            ));
        });
    }

    /**
     * Process defense mechanism for a burg
     */
    private void processBurgDefense(Burg burg, GameState gameState) {
        if (!burg.isAlive()) {
            return;
        }

        if (burg.isAttacking()) {
            Entity currentTarget = burg.getAttackComponent().getAttackContext().getTarget();
            boolean targetInRange = burg.getAttackComponent().inAttackRange();

            if (!currentTarget.isAlive() || !targetInRange) {
                burg.getAttackComponent().setAttackContext(null);
                log.trace("Burg {} target lost, searching for new target...", burg.getStringId());
            } else {
                return;
            }
        }

        // --Find new target--
        findPriorityTargetInRange(burg, gameState).ifPresent(enemy -> {
            log.trace("Burg {} found new target: {}", burg.getStringId(), enemy.getStringId());

            attackService.setAttack(attackContextFactory.createAttackContext(
                gameState.getGameId(), burg.getStringId(), enemy.getStringId(), gameState.getCurrentTick()
            ));
        });
    }

    private Optional<Entity> findPriorityTargetInRange(Tower tower, GameState gameState) {
        return findPriorityTargetInRange(tower, tower.getAttackComponent().getAttackRange(), gameState);
    }
    
    private Optional<Entity> findPriorityTargetInRange(Burg burg, GameState gameState) {
        return findPriorityTargetInRange(burg, burg.getAttackComponent().getAttackRange(), gameState);
    }
    
    /**
     * Find the highest priority target in range for a defensive entity
     * Priority order: Troops first, then Champions
     */
    private Optional<Entity> findPriorityTargetInRange(Entity defender, float attackRange, GameState gameState) {
        // First, look for troops (highest priority)
        Optional<Entity> troopTarget = gameState.getEntities().stream()
            .filter(Entity::isAlive)
            .filter(e -> e.getOwnerSlot() != null)
            .filter(e -> e.getOwnerSlot().getSlot() != defender.getOwnerSlot().getSlot()) // Is an enemy
            .filter(e -> e instanceof Troop)
            .filter(e -> defender.distanceTo(e) <= attackRange)
            .min(Comparator.comparing(e -> defender.distanceTo(e)));

        if (troopTarget.isPresent()) {
            return troopTarget;
        }

        // If no troops found, look for champions
        return gameState.getEntities().stream()
            .filter(Entity::isAlive)
            .filter(e -> e.getOwnerSlot() != null)
            .filter(e -> e.getOwnerSlot().getSlot() != defender.getOwnerSlot().getSlot()) // Is an enemy
            .filter(e -> e instanceof Champion)
            .filter(e -> defender.distanceTo(e) <= attackRange)
            .min(Comparator.comparing(e -> defender.distanceTo(e)));
    }
}
