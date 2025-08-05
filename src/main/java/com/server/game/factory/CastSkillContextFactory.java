package com.server.game.factory;

import com.server.game.model.game.Champion;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.gameState.GameStateService;
import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Data
@Component
@AllArgsConstructor
public class CastSkillContextFactory {
    
    private final GameStateService gameStateService;


    public CastSkillContext createCastSkillContext(
        String gameId, String casterStringId, Vector2 targetPosition, long timestamp) {

        // TODO: Currently, 4 skills do not have a target entity.
        String targetEntityId = null; 

        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            throw new IllegalArgumentException(">>> [AttackContextFactory] GameState not found for gameId: " + gameId);
        }

        Champion caster = (Champion) gameState.getEntityByStringId(casterStringId);

        if (caster == null) {
            throw new IllegalArgumentException(">>> [AttackContextFactory] Caster not found: " + casterStringId);
        }

        if (targetEntityId == null) {
            System.out.println(">>> [AttackContextFactory] Skill no need Target entity");
        }

        Entity target = targetEntityId != null ? gameState.getEntityByStringId(targetEntityId) : null;

        if (targetEntityId != null && target == null) {
            throw new IllegalArgumentException(">>> [AttackContextFactory] Target not found: " + targetEntityId);
        }

        return new CastSkillContext(gameState, caster, target, targetPosition, timestamp);
    }
}
