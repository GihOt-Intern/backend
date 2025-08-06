package com.server.game.service.troop;

import com.server.game.factory.TroopFactory;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.attack.AttackService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.factory.AttackContextFactory;
import com.server.game.factory.MoveContextFactory;
import com.server.game.service.move.MoveService2;
import com.server.game.util.TroopEnum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TroopManager {
    private final GameStateService gameStateService;
    private final TroopFactory troopFactory;

    private final MoveContextFactory moveContextFactory;
    private final MoveService2 moveService;

    
    private final AttackService attackService;
    private final AttackContextFactory attackContextFactory;

    /** 
     * Add a troop instance to the game state.
     */
    public TroopInstance2 createTroop(String gameId, short ownerSlot, TroopEnum troopType, Vector2 position) {
        return troopFactory.createTroop(gameId, ownerSlot, troopType, position);
    }
    
    /**
     * Remove a troop instance (when it dies or is manually removed)
     */
    public boolean removeTroop(String gameId, String troopInstanceId) {
        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for game ID: {}", gameId);
            return false;
        }
        Entity troopInstance = gameStateService.getEntityByStringId(gameId, troopInstanceId);
        gameStateService.removeEntity(gameState, troopInstance);
        return true;
    }

    /** 
     * Attack a target
     */
    public void setAttackTarget(String gameId, String troopInstanceId, String targetId) {
        GameState gameState = gameStateService.getGameStateById(gameId);
        Entity troop = gameStateService.getEntityByStringId(gameState, troopInstanceId);

        if (troop instanceof TroopInstance2) {
            ((TroopInstance2) troop).setInDefensiveStance(false); // Disable defense on manual attack
        }

        AttackContext attackContext = attackContextFactory.createAttackContext(gameId, troopInstanceId, targetId, System.currentTimeMillis());
        attackService.setAttack(attackContext);
    }

    /** 
     * Set move position for a troop instance
     */
    public void setMovePosition(String gameId, String troopInstanceId, Vector2 position) {
        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for game ID: {}", gameId);
            return;
        }
        Entity troopInstance = gameStateService.getEntityByStringId(gameState, troopInstanceId);
        if (troopInstance == null) {
            log.warn("Troop instance not found for ID: {}", troopInstanceId);
            return;
        }
        ((TroopInstance2) troopInstance).updateDefensePosition(position);
        MoveContext moveContext = moveContextFactory.createMoveContext(gameState, troopInstance, position, System.currentTimeMillis());
        moveService.setMove(moveContext, true);
    }
}
