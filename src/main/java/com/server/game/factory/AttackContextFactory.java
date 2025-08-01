package com.server.game.factory;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.AttackContext;
import com.server.game.service.gameState.GameStateService;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Data
@Component
public class AttackContextFactory {
    
    private GameStateService gameStateService;

    public AttackContextFactory(GameStateService gameStateService) {
        this.gameStateService = gameStateService;
    }

    public AttackContext createAttackContext(
        String gameId, String attackerStringId, String targetStringId, long timestamp) {

        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException(">>> [AttackContextFactory] GameState not found for gameId: " + gameId);
        }

        Entity attacker = gameState.getEntityByStringId(attackerStringId);
        Entity target = gameState.getEntityByStringId(targetStringId);

        if (attacker == null) {
            throw new IllegalArgumentException(">>> [AttackContextFactory] Attacker not found: " + attackerStringId);
        }
        if (target == null) {
            throw new IllegalArgumentException(">>> [AttackContextFactory] Target not found: " + targetStringId);
        }

        return new AttackContext(gameState, attacker, target, timestamp);
    }
}
