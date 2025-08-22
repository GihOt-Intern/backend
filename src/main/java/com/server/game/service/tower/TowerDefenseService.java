package com.server.game.service.tower;

import org.springframework.stereotype.Service;

import com.server.game.model.game.GameState;
import com.server.game.service.defense.DefenseService;

import lombok.RequiredArgsConstructor;

/**
 * @deprecated Use {@link com.server.game.service.defense.DefenseService} instead.
 */
@Service
@RequiredArgsConstructor
@Deprecated
public class TowerDefenseService {
    private final DefenseService defenseService;

    /**
     * @deprecated Use {@link com.server.game.service.defense.DefenseService#updateDefenses(GameState)} instead.
     */
    @Deprecated
    public void updateDefenses(GameState gameState) {
        defenseService.updateDefenses(gameState);
    }
    
    /**
     * @deprecated Use {@link com.server.game.service.defense.DefenseService#updateDefenses(GameState)} instead.
     */
    @Deprecated
    public void updateTowerDefenses(GameState gameState) {
        defenseService.updateDefenses(gameState);
    }
}
