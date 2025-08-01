package com.server.game.factory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.util.ChampionEnum;
import com.server.game.util.TroopEnum;
import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.TroopDB;
import com.server.game.resource.service.TroopService;

import lombok.AccessLevel;


@Data
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TroopFactory {

    GameStateService gameStateService;
    ChampionFactory championFactory;
    TroopService troopService;
    SlotStateService slotStateService;

    public TroopInstance2 createTroop(GameState gameState, SlotState ownerSlot, TroopEnum troopEnum) {
        if (gameState == null || ownerSlot == null || troopEnum == null) {
            System.out.println(">>> [TroopFactory] Invalid parameters for creating troop");
            return null;
        }

        TroopDB troopDB = troopService.getTroopDBById(troopEnum);
        TroopInstance2 troopInstance = new TroopInstance2(troopDB, gameState, ownerSlot);

        gameStateService.addEntityTo(gameState, troopInstance);
        slotStateService.addTroop(ownerSlot, troopInstance);

        return troopInstance;
    }
}
