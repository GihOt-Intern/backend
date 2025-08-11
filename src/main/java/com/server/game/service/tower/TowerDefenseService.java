package com.server.game.service.tower;

import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.server.game.factory.AttackContextFactory;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.Tower;
import com.server.game.service.attack.AttackService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TowerDefenseService {
    private final AttackService attackService;
    private final AttackContextFactory attackContextFactory;

    public void updateTowerDefenses(GameState gameState) {
        gameState.getEntities().stream()
            .filter(entity -> entity instanceof Tower)
            .map(entity -> (Tower) entity)
            .forEach(tower -> processTowerDefense(tower, gameState));
    }

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

    private Optional<Entity> findPriorityTargetInRange(Tower tower, GameState gameState) {
        // First, look for troops (highest priority)
        Optional<Entity> troopTarget = gameState.getEntities().stream()
            .filter(Entity::isAlive)
            .filter(e -> e.getOwnerSlot() != null)
            .filter(e -> e.getOwnerSlot().getSlot() != tower.getOwnerSlot().getSlot()) // Is an enemy
            .filter(e -> e.getStringId().startsWith("troop")) // Is a troop
            .filter(e -> tower.distanceToEntityBoundary(e) <= tower.getAttackComponent().getAttackRange())
            .min(Comparator.comparing(e -> tower.distanceToEntityBoundary(e)));

        if (troopTarget.isPresent()) {
            return troopTarget;
        }

        // If no troops found, look for champions
        return gameState.getEntities().stream()
            .filter(Entity::isAlive)
            .filter(e -> e.getOwnerSlot() != null) // Thêm kiểm tra null
            .filter(e -> e.getOwnerSlot().getSlot() != tower.getOwnerSlot().getSlot()) // Is an enemy
            .filter(e -> e.getStringId().startsWith("champion_")) // Is a champion
            .filter(e -> tower.distanceToEntityBoundary(e) <= tower.getAttackComponent().getAttackRange())
            .min(Comparator.comparing(e -> tower.distanceToEntityBoundary(e)));
    }
}
