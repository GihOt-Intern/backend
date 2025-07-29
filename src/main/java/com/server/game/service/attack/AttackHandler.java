package com.server.game.service.attack;

import com.server.game.util.ChampionEnum;

/**
 * Interface to handle attack operations without circular dependency
 */
public interface AttackHandler {
    
    /**
     * Check if a champion can attack (not on cooldown)
     */
    boolean canChampionAttack(String gameId, short attackerSlot, ChampionEnum championEnum);
    
    /**
     * Handle champion attacking another champion
     */
    void handleChampionAttackChampion(String gameId, short attackerSlot, short targetSlot, long timestamp);
    
    /**
     * Handle champion attacking a target/NPC
     */
    void handleChampionAttackTarget(String gameId, short attackerSlot, String targetId, long timestamp);
}
