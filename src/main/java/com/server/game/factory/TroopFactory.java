package com.server.game.factory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.service.move.MoveService;
import com.server.game.util.TroopEnum;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.Troop;
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
    MoveService moveService;

    public Troop createTroop(String gameId, short ownerSlot, TroopEnum troopType, Vector2 spawnPosition) {
        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            return null;
        }

        SlotState slotState = gameState.getSlotState(ownerSlot);
        if (slotState == null) {
            return null;
        }

        TroopDB troopDB = troopService.getTroopDBById(troopType);
        if (troopDB == null) {
            return null;
        }

        if (gameState.peekGold(slotState) < troopDB.getCost()) {
            return null;
        }

        Troop troopInstance = new Troop(
            troopDB,
            gameState,
            slotState
        );

        gameState.spendGold(slotState, troopDB.getCost());

        gameStateService.addEntityTo(gameState, troopInstance);
        slotStateService.addTroop(slotState, troopInstance);
        
        return troopInstance;
    }
}
