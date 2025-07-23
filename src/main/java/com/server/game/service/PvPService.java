package com.server.game.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.MessageSend;
import com.server.game.netty.messageObject.sendObject.pvp.AttackAnimationDisplaySend;
import com.server.game.netty.messageObject.sendObject.pvp.HealthUpdateSend;
import com.server.game.service.gameState.GameStateBroadcastService;
import com.server.game.service.troop.TroopManager;
import com.server.game.service.troop.TroopInstance;

import com.server.game.util.ChampionEnum;
import com.server.game.util.TroopEnum;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PvPService {
    GameStateBroadcastService gameStateBroadcastService;
    TroopManager troopManager;
    
    // Store combat data for each game
    private final Map<String, Map<Short, CombatData>> combatDataMap = new ConcurrentHashMap<>();
    
    /**
     * Handle champion attacking another champion
     */
    public void handleChampionAttackChampion(String gameId, short attackerSlot, short targetSlot, long timestamp) {
        log.info("Champion in slot {} attacking champion in slot {} in game {} at timestamp {}", 
                attackerSlot, targetSlot, gameId, timestamp);
        
        // Get attacker's champion info from slot mapping
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);
        ChampionEnum attackerChampion = slot2ChampionId != null ? slot2ChampionId.get(attackerSlot) : null;
        if (attackerChampion == null) {
            log.warn("No champion found for slot {} in game {}", attackerSlot, gameId);
            return;
        }
        
        // 1. First send attack animation of the attacker
        broadcastAttackerAnimation(gameId, attackerSlot, attackerChampion, timestamp);
        
        // 2. Process the attack and calculate damage
        int damage = processPvPAttack(gameId, attackerSlot, attackerChampion, timestamp);
        
        // 3. Send health update for the target
        broadcastHealthUpdate(gameId, targetSlot, damage, timestamp);
    }
    
    /**
     * Handle champion attacking a target (PvE)
     */
    public void handleChampionAttackTarget(String gameId, short attackerSlot, String targetId, long timestamp) {
        log.info("Champion in slot {} attacking target {} in game {} at timestamp {}", 
                attackerSlot, targetId, gameId, timestamp);
        
        // Get attacker's champion info from slot mapping
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);
        ChampionEnum attackerChampion = slot2ChampionId != null ? slot2ChampionId.get(attackerSlot) : null;
        if (attackerChampion == null) {
            log.warn("No champion found for slot {} in game {}", attackerSlot, gameId);
            return;
        }
        
        // 1. First send attack animation of the attacker
        broadcastAttackerAnimation(gameId, attackerSlot, attackerChampion, timestamp);
        
        // 2. Process PvE attack and get damage
        int damage = processPvEAttack(gameId, targetId, timestamp);
        
        // 3. Send health update for the target
        broadcastHealthUpdateForTarget(gameId, targetId, damage, timestamp);
    }
    
    /**
     * Handle target attacking a champion (PvE counter-attack)
     */
    public void handleTargetAttackChampion(String gameId, String attackerId, short defenderSlot, long timestamp) {
        log.info("Target {} attacking champion in slot {} in game {} at timestamp {}", 
                attackerId, defenderSlot, gameId, timestamp);
        
        // 1. First send attack animation of the target attacker
        broadcastTargetAttackerAnimation(gameId, attackerId, timestamp);
        
        // 2. Process target counter-attack and get damage
        int damage = processTargetCounterAttack(gameId, attackerId, defenderSlot, timestamp);
        
        // 3. Send health update for the champion being attacked
        broadcastHealthUpdate(gameId, defenderSlot, damage, timestamp);
    }
    
    /**
     * Handle troop attacking a player (called by troop AI system)
     */
    public void handleTroopAttackPlayer(String gameId, String troopId, short targetPlayerSlot, long timestamp) {
        log.info("Troop {} attacking player in slot {} in game {} at timestamp {}", 
                troopId, targetPlayerSlot, gameId, timestamp);
        
        // Get the troop instance to calculate damage
        TroopInstance attackingTroop = troopManager.getTroop(gameId, troopId);
        if (attackingTroop == null) {
            log.warn("Attacking troop not found: {} in game {}", troopId, gameId);
            return;
        }
        
        // Calculate troop damage against player
        int damage = calculateTroopDamageToPlayer(attackingTroop);
        
        // 1. Send attack animation
        broadcastTargetAttackerAnimation(gameId, troopId, timestamp);
        
        // 2. Apply damage to player and broadcast health update
        broadcastHealthUpdate(gameId, targetPlayerSlot, damage, timestamp);
        
        log.info("Troop {} dealt {} damage to player slot {}", troopId, damage, targetPlayerSlot);
    }
    
    /**
     * Handle target attacking another target
     */
    public void handleTargetAttackTarget(String gameId, String targetId, short slot, long timestamp) {
        log.info("Target attacking target {} in game {} at timestamp {}", 
                targetId, gameId, timestamp);
        
        // Process target vs target combat
        processTargetVsTarget(gameId, targetId, slot, timestamp);
        
        // Broadcast attack result
        broadcastAttackResult(gameId, slot, "TARGET", "TARGET", timestamp);
    }
    
    /**
     * Process PvP attack between champions
     */
    private int processPvPAttack(String gameId, short attackerSlot, ChampionEnum attackerChampion, long timestamp) {
        // Get or create combat data for the attacker
        CombatData attackerData = getCombatData(gameId, attackerSlot);
        
        // Calculate damage based on champion stats
        int damage = calculateChampionDamage(attackerChampion);
        
        // Update combat statistics
        attackerData.incrementAttacks();
        attackerData.addDamageDealt(damage);
        attackerData.setLastActionTimestamp(timestamp);
        
        log.debug("Champion {} in slot {} dealt {} damage", attackerChampion, attackerSlot, damage);
        
        return damage;
    }
    
    /**
     * Process PvE attack (champion vs target)
     */
    private int processPvEAttack(String gameId, String targetId, long timestamp) {
        // Implement PvE logic here
        log.debug("Processing PvE attack against target {}", targetId);
        
        // Calculate damage based on target type
        int damage = calculatePvEDamage(targetId);
        
        // TODO: Implement target health system, damage calculation, loot drops, etc.
        // For now, just return calculated damage
        
        return damage;
    }
    
    /**
     * Process target counter-attack against champion
     */
    private int processTargetCounterAttack(String gameId, String attackerId, short defenderSlot, long timestamp) {
        // Get combat data for the defender
        CombatData defenderData = getCombatData(gameId, defenderSlot);
        
        // Calculate damage received
        int damage = calculateTargetDamage(attackerId);
        
        // Update defender's combat data
        defenderData.addDamageReceived(damage);
        defenderData.setLastActionTimestamp(timestamp);
        
        log.debug("Target {} dealt {} damage to champion in slot {}", attackerId, damage, defenderSlot);
        
        return damage;
    }
    
    /**
     * Process target vs target combat
     */
    private void processTargetVsTarget(String gameId, String targetId, short slot, long timestamp) {
        // Implement target vs target logic
        log.debug("Processing target vs target combat for target {}", targetId);
        
        // TODO: Implement target AI combat system
    }
    
    /**
     * Calculate damage based on champion type and stats
     */
    private int calculateChampionDamage(ChampionEnum championEnum) {
        // Get champion base damage from service
        // This is a simplified calculation - in a real game you'd consider:
        // - Base attack damage
        // - Equipment/items
        // - Buffs/debuffs
        // - Critical hit chance
        // - Resistance/armor of target
        
        switch (championEnum) {
            case MARKSMAN_CROSSBOW:
                return 80 + (int) (Math.random() * 20); // 80-100 damage
            case MELEE_AXE:
                return 100 + (int) (Math.random() * 30); // 100-130 damage
            case ASSASSIN_SWORD:
                return 60 + (int) (Math.random() * 15); // 60-75 damage
            case MAGE_SCEPTER:
                return 90 + (int) (Math.random() * 25); // 90-115 damage
            default:
                return 50; // Default damage
        }
    }
    
    /**
     * Calculate damage from a target/monster
     */
    private int calculateTargetDamage(String targetId) {
        // Simplified target damage calculation
        // In a real game, this would be based on target type, level, etc.
        return 30 + (int) (Math.random() * 20); // 30-50 damage
    }
    
    /**
     * Calculate damage dealt to PvE targets (troops)
     */
    private int calculatePvEDamage(String targetId) {
        // For troop targets, use a base damage with some randomness
        // In a real implementation, this would consider the attacking champion's stats
        
        // Check if this is a troop instance ID (UUID format)
        if (targetId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            // Base champion attack damage against troops
            int baseDamage = 75; // Base damage for champions attacking troops
            int variation = (int) (baseDamage * 0.3); // ±30% variation
            return baseDamage + (int) (Math.random() * variation * 2) - variation;
        }
        
        // Fallback for non-troop targets (legacy system)
        if (targetId.startsWith("boss_")) {
            return 150 + (int) (Math.random() * 50); // 150-200 damage to bosses
        } else if (targetId.startsWith("elite_")) {
            return 100 + (int) (Math.random() * 30); // 100-130 damage to elite enemies
        } else {
            return 80 + (int) (Math.random() * 20); // 80-100 damage to normal enemies
        }
    }
    
    /**
     * Calculate damage dealt by a troop to a player
     */
    private int calculateTroopDamageToPlayer(TroopInstance attackingTroop) {
        // Get base damage from troop type configuration
        // This would typically come from the troop's attack stat in JSON config
        int baseDamage = switch (attackingTroop.getTroopType()) {
            case AXIS -> 45;      // Tank troops do moderate damage
            case SHADOW -> 80;    // Assassin troops do high damage  
            case CROSSBAWL -> 60; // Ranged troops do good damage
            case HEALER -> 25;    // Healers do low damage
        };
        
        // Apply damage multiplier if troop is buffed
        if (attackingTroop.isBuffed()) {
            baseDamage = (int) (baseDamage * attackingTroop.getDamageMultiplier());
        }
        
        // Add some randomness (±20%)
        int variation = (int) (baseDamage * 0.2);
        return baseDamage + (int) (Math.random() * variation * 2) - variation;
    }
    
    /**
     * Get or create combat data for a player
     */
    private CombatData getCombatData(String gameId, short slot) {
        return combatDataMap
                .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(slot, k -> new CombatData());
    }
    
    /**
     * Broadcast attack result to all players in the game
     */
    private void broadcastAttackResult(String gameId, Short targetSlot, String attackerType, String targetType, long timestamp) {
        String message = String.format("Combat: %s attacked %s at %d", attackerType, targetType, timestamp);
        if (targetSlot != null) {
            message += String.format(" (Target slot: %d)", targetSlot);
        }
        
        MessageSend attackResult = new MessageSend(message);
        
        // Get all channels in the game and broadcast
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(attackResult);
                }
            });
        }
    }
    
    /**
     * Get combat statistics for a player
     */
    public CombatData getPlayerCombatData(String gameId, short slot) {
        Map<Short, CombatData> gameData = combatDataMap.get(gameId);
        return gameData != null ? gameData.get(slot) : null;
    }
    
    /**
     * Clear combat data for a game (when game ends)
     */
    public void clearGameCombatData(String gameId) {
        combatDataMap.remove(gameId);
        log.info("Cleared combat data for game {}", gameId);
    }
    
    /**
     * Broadcast attacker animation to all players in the game
     */
    private void broadcastAttackerAnimation(String gameId, short attackerSlot, ChampionEnum attackerChampion, long timestamp) {
        log.debug("Broadcasting attack animation for slot {} with champion {}", attackerSlot, attackerChampion);
        
        String animationType = getAttackAnimationType(attackerChampion);
        
        // Create attack animation display message
        AttackAnimationDisplaySend attackAnimation = new AttackAnimationDisplaySend(
                attackerSlot, animationType, timestamp);
        
        // Broadcast to all players in the game
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(attackAnimation);
                }
            });
        }
    }
    
    /**
     * Broadcast target attacker animation to all players in the game
     */
    private void broadcastTargetAttackerAnimation(String gameId, String attackerId, long timestamp) {
        log.debug("Broadcasting target attack animation for attacker {}", attackerId);
        
        String animationType = getTargetAnimationType(attackerId);
        
        // Create attack animation display message
        AttackAnimationDisplaySend attackAnimation = new AttackAnimationDisplaySend(
                attackerId, animationType, timestamp);
        
        // Broadcast to all players in the game
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(attackAnimation);
                }
            });
        }
    }
    
    /**
     * Broadcast health update for a champion
     */
    private void broadcastHealthUpdate(String gameId, short targetSlot, int damage, long timestamp) {
        log.debug("Broadcasting health update for slot {} with damage {}", targetSlot, damage);
        
        // Use the enhanced broadcast service
        gameStateBroadcastService.broadcastHealthUpdate(gameId, targetSlot, damage, timestamp);
    }
    
    /**
     * Broadcast health update for a target/NPC
     */
    private void broadcastHealthUpdateForTarget(String gameId, String targetId, int damage, long timestamp) {
        log.debug("Broadcasting health update for target {} with damage {}", targetId, damage);
        
        // Get the actual troop instance from TroopManager
        TroopInstance targetTroop = troopManager.getTroop(gameId, targetId);
        if (targetTroop == null) {
            log.warn("Target troop not found: {} in game {}", targetId, gameId);
            return;
        }
        
        // Apply damage to the troop
        targetTroop.takeDamage(damage);
        
        // Get actual health values after damage
        int currentHealth = targetTroop.getCurrentHP();
        int maxHealth = targetTroop.getMaxHP();
        
        log.info("Target {} took {} damage - HP: {}/{}", targetId, damage, currentHealth, maxHealth);
        
        // If troop died, remove it from the game
        if (!targetTroop.isAlive()) {
            troopManager.removeTroop(gameId, targetId);
            log.info("Target troop {} has been killed and removed from game {}", targetId, gameId);
        }
        
        HealthUpdateSend healthUpdate = new HealthUpdateSend(targetId, currentHealth, maxHealth, damage, timestamp);
        
        // Broadcast to all players in the game
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(healthUpdate);
                }
            });
            
            log.info("Broadcasted health update for target {} in game {} - HP: {}/{} (damage: {})", 
                    targetId, gameId, currentHealth, maxHealth, damage);
        }
    }
    /**
     * Get attack animation type based on champion enum
     */
    private String getAttackAnimationType(ChampionEnum championEnum) {
        switch (championEnum) {
            case MARKSMAN_CROSSBOW:
                return "crossbow_shot";
            case MELEE_AXE:
                return "axe_swing";
            case ASSASSIN_SWORD:
                return "sword_slash";
            case MAGE_SCEPTER:
                return "magic_missile";
            default:
                return "basic_attack";
        }
    }

    /**
     * Get target animation type based on target ID
     */
    private String getTargetAnimationType(String targetId) {
        if (targetId.startsWith("boss_")) {
            return "boss_attack";
        } else if (targetId.startsWith("elite_")) {
            return "elite_attack";
        } else {
            return "target_attack";
        }
    }

    /**
     * Inner class to store combat statistics
     */
    public static class CombatData {
        private int attacks = 0;
        private int damageDealt = 0;
        private int damageReceived = 0;
        private long lastActionTimestamp = 0;
        
        public void incrementAttacks() {
            this.attacks++;
        }
        
        public void addDamageDealt(int damage) {
            this.damageDealt += damage;
        }
        
        public void addDamageReceived(int damage) {
            this.damageReceived += damage;
        }
        
        public void setLastActionTimestamp(long timestamp) {
            this.lastActionTimestamp = timestamp;
        }
        
        // Getters
        public int getAttacks() { return attacks; }
        public int getDamageDealt() { return damageDealt; }
        public int getDamageReceived() { return damageReceived; }
        public long getLastActionTimestamp() { return lastActionTimestamp; }
    }
}
