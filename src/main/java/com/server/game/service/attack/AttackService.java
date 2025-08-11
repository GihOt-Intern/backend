package com.server.game.service.attack;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.AttackContext;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttackService {

    public void setAttack(AttackContext ctx) {
        Entity attacker = ctx.getAttacker();

        attacker.setAttackContext(ctx);

        log.debug("Set attack context for entity {}: {}", attacker.getStringId(), ctx);
    }


    public void processAttacks(GameState gameState) {
        Set<Entity> entities = new HashSet<>(gameState.getEntities());
        for (Entity entity : entities) {
            this.processAttackOf(entity);
        }
    }

    private void processAttackOf(Entity attacker) {
        // Perform the attack
        attacker.performAttack();
    }

    public void setStopAttacking(Entity entity) {
        entity.setAttackContext(null);
        log.debug("Stopping attack for entity {}", entity.getStringId());
    }


    public void clearGameAttackContexts(GameState gameState) {
        Set<Entity> entities = new HashSet<>(gameState.getEntities());
        for (Entity entity : entities) {
            entity.setAttackContext(null);
        }
    }
}
