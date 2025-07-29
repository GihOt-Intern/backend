package com.server.game.service.goldGeneration;


import org.springframework.stereotype.Service;

import com.server.game.service.gameState.GameStateService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GoldGenerationService {

    GameStateService gameStateService;

    public void generateGold(String gameId) {
        gameStateService.genGold(gameId);
    }
}