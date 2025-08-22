package com.server.game.factory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.ChampionEnum;
import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.building.Burg;
import com.server.game.model.game.building.Tower;
import com.server.game.resource.model.SlotInfo.BurgDB;
import com.server.game.resource.model.SlotInfo.TowerDB;

import lombok.AccessLevel;

@Slf4j
@Data
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SlotStateFactory {

    GameStateService gameStateService;
    ChampionFactory championFactory;
    TowerFactory towerFactory;
    BurgFactory burgFactory;

    public SlotState createSlotState(GameState gameState, Short slot, ChampionEnum championEnum) {
        if (gameState == null || slot == null || championEnum == null) {
            log.info(">>> [SlotStateFactory] Invalid parameters for creating slot state");
            return null;
        }

        Integer initialGold = gameState.getInitialGold();

        // Initialize the slot state with temporaty null for champion and towers
        // They will be set later after creating the Champion and Towers.
        SlotState slotState = new SlotState(gameState, slot, null, null, null, initialGold);


        // Create the Champion for this slot
        Champion champion = championFactory.createChampion(championEnum, gameState, slotState);

        if (champion == null) {
            log.info(">>> [SlotStateFactory] Champion creation failed for slot " + slot);
            return null;
        }

        slotState.setChampion(champion);

        gameStateService.addEntityTo(gameState, champion);

        // Create the Towers for this slot
        Set<TowerDB> towerDBs = gameState.getGameMap().getTowers(slot);
        Set<Tower> towers = towerDBs.stream()
                .map(towerDB -> {
                    Tower tower = towerFactory.createTower(gameState, slotState, towerDB);
                    if (tower == null) {
                        log.info(">>> [SlotStateFactory] Tower creation failed for slot " + slot);
                        return null;
                    }
                    gameStateService.addEntityTo(gameState, tower);
                    return tower;
                })
                .collect(Collectors.toSet());

        slotState.setTowers(towers);


        // Create the Burg for this slot
        BurgDB burgDB = gameState.getGameMap().getBurgDB(slot);
        Burg burg = burgFactory.createBurg(gameState, slotState, burgDB);
        if (burg == null) {
            log.info(">>> [SlotStateFactory] Burg creation failed for slot " + slot);
            return null;
        }
        slotState.setBurg(burg);
        gameStateService.addEntityTo(gameState, burg);
        
        return slotState;        
    }
}
