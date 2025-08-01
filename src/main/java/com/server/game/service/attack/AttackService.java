package com.server.game.service.attack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.AttackContext;
import com.server.game.service.move.MoveService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttackService {

    MoveService moveService;

    
    // Store attack context for each entity in a game
    private final Map<GameState, Map<Entity, AttackContext>> attackCtxs = new ConcurrentHashMap<>();


    //******* MAPS PRIVATE INTERFACE *********//
    private void pushAttackContext2Map(Entity entity, AttackContext ctx) {
        // Push the attack context to the map
        GameState gameState = entity.getGameState();
        Map<Entity, AttackContext> gameAttackCtxs =
            attackCtxs.computeIfAbsent(gameState, k -> new ConcurrentHashMap<>());
        gameAttackCtxs.put(entity, ctx);
    }

    private AttackContext peekAttackContextFromMap(Entity entity) {
        // Get the attack context from the map
        GameState gameState = entity.getGameState();
        Map<Entity, AttackContext> gameAttackCtxs = attackCtxs.get(gameState);
        if (gameAttackCtxs == null) {
            return null;
        }
        return gameAttackCtxs.get(entity);
    }

    private void removeAttackContextFromMap(Entity entity) {
        GameState gameState = entity.getGameState();
        Map<Entity, AttackContext> gameAttackCtxs = attackCtxs.get(gameState);
        if (gameAttackCtxs != null) {
            gameAttackCtxs.remove(entity);
        }
        if (gameAttackCtxs == null || gameAttackCtxs.isEmpty()) {
            attackCtxs.remove(gameState);
        }
    }
//******* END MAPS PRIVATE INTERFACE *********//

    public void setAttack(AttackContext ctx) {
        Entity attacker = ctx.getAttacker();
        this.pushAttackContext2Map(attacker, ctx);

        log.debug("Set attack context for entity {}: {}", attacker.getStringId(), ctx);

        this.setStick2Target(attacker, ctx.getTarget());
    }

    private void setStick2Target(Entity attacker, Entity target) {
        moveService.setMove(attacker, target.getCurrentPosition());
    }


    public void processAttacks(GameState gameState) {
        Set<Entity> entities = new HashSet<>(gameState.getEntities());
        for (Entity entity : entities) {
            this.processAttackOf(entity);
        }
    }

    private void processAttackOf(Entity attacker) {
        AttackContext ctx = this.peekAttackContextFromMap(attacker);
        if (ctx == null) {
            return; // No attack context found for this entity
        }

        // Perform the attack
        attacker.performAttack(ctx);

        // After performing the attack, check if the target is still alive
        if (!ctx.getTarget().isAlive()) {
            log.debug("Target {} is dead, removing attack context for entity {}", ctx.getTarget().getStringId(), attacker.getStringId());
            this.removeAttackContextFromMap(attacker);

            this.setUnstickFromTarget(attacker);
        }
    }

    private void setUnstickFromTarget(Entity attacker) {
        // Unstick the entity from the target
        // Stop moving towards the target
        moveService.setMove(attacker, attacker.getCurrentPosition());
    }

    public boolean isAttacking(Entity entity) {
        AttackContext ctx = this.peekAttackContextFromMap(entity);
        return ctx != null;
    }

    public void stopAttack(Entity entity) {
        this.removeAttackContextFromMap(entity);
    }

    public void clearGameAttackContexts(GameState gameState) {
        attackCtxs.remove(gameState);
    }
}
