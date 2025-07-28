package com.server.game.netty.receiveMessageHandler.pvpHandler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.receiveObject.pvp.ChampionChampionReceive;
import com.server.game.netty.messageObject.receiveObject.pvp.ChampionTarget;
import com.server.game.netty.messageObject.receiveObject.pvp.TargetChampion;
import com.server.game.netty.messageObject.receiveObject.pvp.TargetTarget;
import com.server.game.service.AttackTargetingService;
import com.server.game.service.PvPService;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * PvPHandler is responsible for handling Player vs Player and Player vs Environment interactions.
 * This class processes combat messages and delegates business logic to PvPService.
 */
@Slf4j
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PvPHandler {
    
    PvPService pvpService;
    AttackTargetingService attackTargetingService;
    
    /**
     * Handle champion attacking another champion (PvP)
     */
    @MessageMapping(ChampionChampionReceive.class)
    public void handleChampionAttackChampion(ChampionChampionReceive receiveObject, Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        short attackerSlot = receiveObject.getSlot();
        short targetSlot = receiveObject.getTargetSlot();
        long timestamp = receiveObject.getTimestamp();
        
        if (gameId == null) {
            log.warn("No game ID found for channel when processing champion vs champion attack");
            return;
        }
        
        // Validate that the slot matches the channel's assigned slot (anti-cheat)
        short expectedSlot = ChannelManager.getSlotByChannel(channel);
        if (attackerSlot != expectedSlot) {
            log.warn("Slot mismatch in champion attack: expected {}, received {}", expectedSlot, attackerSlot);
            return;
        }
        
        log.info("Processing champion vs champion attack from slot {} to slot {} in game {} at timestamp {}", 
                attackerSlot, targetSlot, gameId, timestamp);
        
        // Set attack target using the new utility method - this will trigger optimized movement towards target
        attackTargetingService.setChampionAttackTarget(gameId, attackerSlot, targetSlot);
        
        // Check if already in range and can attack immediately
        if (attackTargetingService.isInAttackRange(gameId, attackerSlot)) {
            pvpService.handleChampionAttackChampion(gameId, attackerSlot, targetSlot, timestamp);
        }
    }
    
    /**
     * Handle champion attacking a target (PvE)
     */
    @MessageMapping(ChampionTarget.class)
    public void handleChampionAttackTarget(ChampionTarget receiveObject, Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        String targetId = receiveObject.getTargetId();
        long timestamp = receiveObject.getTimestamp();
        short attackerSlot = ChannelManager.getSlotByChannel(channel);
        
        if (gameId == null) {
            log.warn("No game ID found for channel when processing champion vs target attack");
            return;
        }
        
        log.info("Processing champion vs target attack against {} in game {} at timestamp {}", 
                targetId, gameId, timestamp);
        
        // Set attack target using the new utility method - this will trigger movement towards target
        attackTargetingService.setTargetAttackTarget(gameId, attackerSlot, targetId);
        
        // Check if already in range and can attack immediately
        if (attackTargetingService.isInAttackRange(gameId, attackerSlot)) {
            pvpService.handleChampionAttackTarget(gameId, attackerSlot, targetId, timestamp);
        }
    }
    
    /**
     * Handle target attacking a champion (PvE counter-attack)
     */
    @MessageMapping(TargetChampion.class)
    public void handleTargetAttackChampion(TargetChampion receiveObject, Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        String attackerId = receiveObject.getAttackerId();
        short defenderSlot = receiveObject.getSlot();
        long timestamp = receiveObject.getTimestamp();
        
        if (gameId == null) {
            log.warn("No game ID found for channel when processing target vs champion attack");
            return;
        }
        
        log.info("Processing target vs champion attack from {} against slot {} in game {} at timestamp {}", 
                attackerId, defenderSlot, gameId, timestamp);
        
        pvpService.handleTargetAttackChampion(gameId, attackerId, defenderSlot, timestamp);
    }
    
    /**
     * Handle target attacking another target
     */
    @MessageMapping(TargetTarget.class)
    public void handleTargetAttackTarget(TargetTarget receiveObject, Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        String targetId = receiveObject.getTargetId();
        short slot = receiveObject.getSlot();
        long timestamp = receiveObject.getTimestamp();
        
        if (gameId == null) {
            log.warn("No game ID found for channel when processing target vs target attack");
            return;
        }
        
        log.info("Processing target vs target attack against {} in game {} at timestamp {}", 
                targetId, gameId, timestamp);
        
        pvpService.handleTargetAttackTarget(gameId, targetId, slot, timestamp);
    }
}
