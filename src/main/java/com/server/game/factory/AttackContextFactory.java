package com.server.game.factory;

import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.context.AttackContext;
import com.server.game.netty.messageHandler.PlaygroundMessageHandler;
import com.server.game.netty.receiveObject.attack.AttackReceive;
import com.server.game.resource.model.ChampionDB;
import com.server.game.resource.service.GameMapService;
import com.server.game.service.champion.ChampionService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class AttackContextFactory {
    
    GameStateService gameStateService;

    public AttackContext createAttackContext(AttackReceive attackReceive) {
        return null;
    }
}
