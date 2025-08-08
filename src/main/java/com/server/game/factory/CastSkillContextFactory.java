package com.server.game.factory;

import com.server.game.model.game.Champion;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.component.attackComponent.SkillReceiver;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.gameState.GameStateService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Data
@Component
@AllArgsConstructor
@Slf4j
public class CastSkillContextFactory {
    
    private final GameStateService gameStateService;


    public CastSkillContext createCastSkillContext(
        String gameId, String casterStringId, Vector2 targetPosition, long timestamp) {

        // TODO: Currently, 4 skills do not have a target entity.
        String targetEntityId = null; 

        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            log.info("GameState not found for gameId: {}", gameId);
        }

        Champion caster = (Champion) gameState.getEntityByStringId(casterStringId);

        if (caster == null) {
            log.info("Caster not found: {}", casterStringId);
        }

        if (targetEntityId == null) {
            log.info("Skill no need Target entity");
        }

        Entity target = targetEntityId != null ? gameState.getEntityByStringId(targetEntityId) : null;

        if (targetEntityId != null && target == null) {
            log.info("Target not found: {}", targetEntityId);
        }

        if ((target != null) && !(target instanceof SkillReceiver skillReceiver)) {
            log.info("Target must be a SkillReceiver");
        }

        return new CastSkillContext(gameState, caster, (SkillReceiver) target, targetPosition, timestamp);
    }
}
