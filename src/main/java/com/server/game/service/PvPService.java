package com.server.game.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.MessageSend;
import com.server.game.netty.messageObject.sendObject.pvp.AttackAnimationDisplaySend;
import com.server.game.netty.messageObject.sendObject.pvp.HealthUpdateSend;

import com.server.game.util.ChampionEnum;

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
     * Calculate damage dealt to PvE targets
     */
    private int calculatePvEDamage(String targetId) {
        // Simplified PvE damage calculation
        // In a real game, this would consider champion stats, target armor, etc.
        if (targetId.startsWith("boss_")) {
            return 150 + (int) (Math.random() * 50); // 150-200 damage to bosses
        } else if (targetId.startsWith("elite_")) {
            return 100 + (int) (Math.random() * 30); // 100-130 damage to elite enemies
        } else {
            return 80 + (int) (Math.random() * 20); // 80-100 damage to normal enemies
        }
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
        
        // TODO: Get actual health values from game state/health management system
        int maxHealth = 1000; // Placeholder value
        int currentHealth = Math.max(0, maxHealth - damage); // Simplified calculation
        
        HealthUpdateSend healthUpdate = new HealthUpdateSend(targetSlot, currentHealth, maxHealth, damage, timestamp);
        
        // Broadcast to all players in the game
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(healthUpdate);
                }
            });
        }
    }
    
    /**
     * Broadcast health update for a target/NPC
     */
    private void broadcastHealthUpdateForTarget(String gameId, String targetId, int damage, long timestamp) {
        log.debug("Broadcasting health update for target {} with damage {}", targetId, damage);
        
        // TODO: Get actual health values from target/NPC management system
        int maxHealth = calculateTargetMaxHealth(targetId);
        int currentHealth = Math.max(0, maxHealth - damage); // Simplified calculation
        
        HealthUpdateSend healthUpdate = new HealthUpdateSend(targetId, currentHealth, maxHealth, damage, timestamp);
        
        // Broadcast to all players in the game
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels != null) {
            gameChannels.forEach(channel -> {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(healthUpdate);
                }
            });
        }
    }
    
    /**
     * Calculate max health for target based on type
     */
    private int calculateTargetMaxHealth(String targetId) {
        if (targetId.startsWith("boss_")) {
            return 5000; // Boss targets have more health
        } else if (targetId.startsWith("elite_")) {
            return 2000; // Elite targets have moderate health
        } else {
            return 1000; // Normal targets have standard health
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
