package com.server.game.factory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.ChampionEnum;


import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.map.component.Vector2;

import lombok.AccessLevel;


@Data
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SlotStateFactory {

    GameStateService gameStateService;
    ChampionFactory championFactory;

    public SlotState createSlotState(GameState gameState, Short slot, ChampionEnum championEnum) {
        if (gameState == null || slot == null || championEnum == null) {
            System.out.println(">>> [SlotStateFactory] Invalid parameters for creating slot state");
            return null;
        }

        Vector2 initialPosition = gameState.getSpawnPosition(slot);
        Integer initialGold = gameState.getInitialGold();


        SlotState slotState = new SlotState(slot, null, initialPosition, initialGold);


        Champion champion = championFactory.createChampion(championEnum, gameState, slotState);

        if (champion == null) {
            System.out.println(">>> [SlotStateFactory] Champion creation failed for slot " + slot);
            return null;
        }

        slotState.setChampion(champion);

        gameStateService.addEntityTo(gameState, champion);

        return slotState;        
    }
}
