package com.server.game.service.troop;

import com.server.game.factory.TroopFactory;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.entity.EntityDeathSend;
import com.server.game.service.attack.AttackService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.factory.AttackContextFactory;
import com.server.game.factory.MoveContextFactory;
import com.server.game.service.move.MoveService2;
import com.server.game.util.TroopEnum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import io.netty.channel.Channel;

@Slf4j
@Service
@RequiredArgsConstructor
public class TroopManager {
    private final GameStateService gameStateService;
    private final SlotStateService slotStateService;
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
        if (troopInstance == null || !(troopInstance instanceof TroopInstance2)) {
            log.warn("Troop instance not found for ID: {}", troopInstanceId);
            return false;
        }

        TroopInstance2 troop = (TroopInstance2) troopInstance;
        
        // Remove from SlotState first
        slotStateService.removeTroop(troop.getOwnerSlot(), troop);
        
        // Remove from GameState
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
        
        TroopInstance2 troop = (TroopInstance2) troopInstance;
        
        // Update defense position but do NOT enable defensive stance for manual moves
        troop.updateDefensePosition(position);
        troop.setInDefensiveStance(false); // Disable defensive stance on manual move
        
        MoveContext moveContext = moveContextFactory.createMoveContext(gameState, troopInstance, position, System.currentTimeMillis());
        moveService.setMove(moveContext, true);
        
        log.debug("Manual move set for troop {} to position {}. Defensive stance disabled.", troopInstanceId, position);
    }

    /**
     * Check if a troop has died and handle death logic
     * @return true if troop died, false otherwise
     */
    public boolean checkAndHandleTroopDeath(String gameId, String troopInstanceId) {
        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for game ID: {}", gameId);
            return false;
        }

        Entity troopEntity = gameStateService.getEntityByStringId(gameState, troopInstanceId);
        if (troopEntity == null || !(troopEntity instanceof TroopInstance2)) {
            log.warn("Troop instance not found for ID: {}", troopInstanceId);
            return false;
        }

        TroopInstance2 troop = (TroopInstance2) troopEntity;
        if (troop.getCurrentHP() > 0) {
            return false; // Troop is still alive
        }

        log.info("Troop {} has died in game {}", troopInstanceId, gameId);

        // Remove the troop from the game state
        this.removeTroop(gameId, troopInstanceId);

        // Send death message to all clients
        this.sendTroopDeathMessage(gameId, troopInstanceId);

        return true;
    }

    /**
     * Check all troops in a game for deaths and handle them
     * This is more efficient than checking troops one by one
     */
    public void checkAndHandleAllTroopDeaths(String gameId) {
        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for game ID: {}", gameId);
            return;
        }

        // Collect all dead troops to avoid concurrent modification
        var deadTroops = gameState.getEntities().stream()
            .filter(entity -> entity.getStringId().startsWith("troop_"))
            .filter(entity -> entity.getCurrentHP() <= 0)
            .map(Entity::getStringId)
            .toList();

        // Process each dead troop
        for (String troopId : deadTroops) {
            try {
                this.checkAndHandleTroopDeath(gameId, troopId);
            } catch (Exception e) {
                log.error("Error processing death for troop {} in game {}: {}", troopId, gameId, e.getMessage(), e);
            }
        }

        if (!deadTroops.isEmpty()) {
            log.info("Processed {} troop deaths in game {}", deadTroops.size(), gameId);
        }
    }

    /**
     * Send troop death message to all clients in the game
     */
    private void sendTroopDeathMessage(String gameId, String troopInstanceId) {
        EntityDeathSend deathMessage = new EntityDeathSend(troopInstanceId);
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            channel.writeAndFlush(deathMessage);
            log.info("Sent troop death message for gameId: {}, troopId: {}", gameId, troopInstanceId);
        } else {
            log.warn("No channel found for gameId: {} when sending troop death message", gameId);
        }
    }
}
