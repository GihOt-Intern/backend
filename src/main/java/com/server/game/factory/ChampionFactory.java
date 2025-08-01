package com.server.game.factory;

import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.netty.messageHandler.PlaygroundMessageHandler;
import com.server.game.resource.model.ChampionDB;
import com.server.game.service.champion.ChampionService;
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
public class ChampionFactory {
    
    ChampionService championService;
    PlaygroundMessageHandler playgroundHandler;

    public Champion createChampion(ChampionEnum championEnum, GameState gameState, SlotState ownerSlot) {
        ChampionDB championDB = championService.getChampionDBById(championEnum);
        if (championDB == null) {
            return null;
        }
        return new Champion(championDB, ownerSlot, gameState, playgroundHandler);
    }
}
