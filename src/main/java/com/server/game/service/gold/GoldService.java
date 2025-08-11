package com.server.game.service.gold;


import org.springframework.stereotype.Service;

import com.server.game.factory.GoldMineFactory;
import com.server.game.model.game.GameState;
import com.server.game.model.game.GoldMine;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMap.Playground;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GoldService {

    GameStateService gameStateService;
    GoldMineFactory goldMineFactory;

    public void autoIncreaseGold(GameState gameState) {
        gameStateService.autoIncreaseGold4SlotsInPlayground(gameState);
    }

    public void randomlyGenerateGoldMine(GameState gameState) {
        if (gameState == null) {
            log.error("GameState is null, cannot generate gold mine.");
            return;
        }
        if (!gameState.inGoldMineGenerationTick()) {
            // log.info("Not in gold mine generation tick, skipping gold mine generation. Current tick: {}, next generation tick: {}",
            //     gameState.getCurrentTick(), gameState.getNextGoldMineGenerationTick());
            return;
        }

        boolean isSmallGoldMine = Util.randomBoolean();
        Playground playground = gameState.getPlayground();
        Vector2 playgroundCenter = playground.getPosition();
        Float playgroundLength = playground.getLength();
        Float playgroundWidth = playground.getWidth();

        Vector2 randomGoldMinePosition = null;
        
        do {
            randomGoldMinePosition = new Vector2(
                Util.randomFloat(
                    playgroundCenter.x() - (playgroundLength / 2), 
                    playgroundCenter.x() + (playgroundLength / 2)),
                Util.randomFloat(
                    playgroundCenter.y() - (playgroundWidth / 2), 
                    playgroundCenter.y() + (playgroundWidth / 2))
            );
        } while (!gameState.isWalkable(randomGoldMinePosition));

        // this method already push the gold mine to gameState's entities list
        GoldMine goldMine = goldMineFactory.createGoldMine(gameState, isSmallGoldMine, randomGoldMinePosition);

        gameState.getGameStateService()
            .sendGoldMineSpawnMessage(gameState.getGameId(), goldMine.getStringId(), isSmallGoldMine, randomGoldMinePosition, goldMine.getInitialHP());

        gameState.updateNextGoldMineGenerationTick();
    }       
        
}