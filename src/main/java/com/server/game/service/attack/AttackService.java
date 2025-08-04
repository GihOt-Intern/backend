package com.server.game.service.attack;

import java.util.HashSet;
import java.util.Set;

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

    // No need map anymore


//     //******* MAPS PRIVATE INTERFACE *********//
//     private void pushAttackContext2Map(Entity entity, AttackContext ctx) {
//         // Push the attack context to the map
//         GameState gameState = entity.getGameState();
//         Map<Entity, AttackContext> gameAttackCtxs =
//             attackCtxs.computeIfAbsent(gameState, k -> new ConcurrentHashMap<>());
//         gameAttackCtxs.put(entity, ctx);
//     }

//     private AttackContext peekAttackContextFromMap(Entity entity) {
//         // Get the attack context from the map
//         GameState gameState = entity.getGameState();
//         Map<Entity, AttackContext> gameAttackCtxs = attackCtxs.get(gameState);
//         if (gameAttackCtxs == null) {
//             return null;
//         }
//         return gameAttackCtxs.get(entity);
//     }

//     private void removeAttackContextFromMap(Entity entity) {
//         GameState gameState = entity.getGameState();
//         Map<Entity, AttackContext> gameAttackCtxs = attackCtxs.get(gameState);
//         if (gameAttackCtxs != null) {
//             gameAttackCtxs.remove(entity);
//         }
//         if (gameAttackCtxs == null || gameAttackCtxs.isEmpty()) {
//             attackCtxs.remove(gameState);
//         }
//     }
// //******* END MAPS PRIVATE INTERFACE *********//

    public void setAttack(AttackContext ctx) {
        Entity attacker = ctx.getAttacker();

        attacker.setAttackContext(ctx);

        log.debug("Set attack context for entity {}: {}", attacker.getStringId(), ctx);

        attacker.setStick2Target(ctx.getTarget());
    }


    public void processAttacks(GameState gameState) {
        Set<Entity> entities = new HashSet<>(gameState.getEntities());
        for (Entity entity : entities) {
            this.processAttackOf(entity);
        }
    }

    private void processAttackOf(Entity attacker) {

        // Perform the attack
        boolean didAttack = attacker.performAttack();

        if(!didAttack) { return; }
        
        AttackContext ctx = attacker.getAttackContext();
        // After performing the attack, check if the target is still alive
        if (!ctx.getTarget().isAlive()) {
            log.debug("Target {} is dead, removing attack context for entity {}", ctx.getTarget().getStringId(), attacker.getStringId());

            this.setUnstickFromTarget(attacker);
        }
    }

    public boolean isAttacking(Entity entity) {
        return entity.getAttackContext() != null;
    }

    public void stopAttack(Entity entity) {
        entity.setAttackContext(null);
        log.debug("Stopping attack for entity {}", entity.getStringId());
    }

    private void setUnstickFromTarget(Entity attacker) {
        // Unstick the entity from the target
        // Stop moving towards the target
        moveService.setMove(attacker, attacker.getCurrentPosition(), true);
    }

    public void clearGameAttackContexts(GameState gameState) {
        Set<Entity> entities = new HashSet<>(gameState.getEntities());
        for (Entity entity : entities) {
            entity.setAttackContext(null);
        }
    }
}
