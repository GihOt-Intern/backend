package com.server.game.factory;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.gameState.GameStateService;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class MoveContextFactory {

    GameStateService gameStateService;

    public MoveContext createMoveContext(GameState gameState, Entity mover, Vector2 targetPoint, long timestamp) {
        // Create and return the MoveContext
        return new MoveContext(gameState, mover, targetPoint, timestamp);
    }
}
