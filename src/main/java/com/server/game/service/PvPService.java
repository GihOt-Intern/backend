package com.server.game.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.server.game.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.pvp.AttackAnimationDisplaySend;
import com.server.game.netty.messageObject.sendObject.pvp.HealthUpdateSend;
import com.server.game.resource.model.Champion;
import com.server.game.resource.service.ChampionService;
import com.server.game.service.MoveService.PositionData;
import com.server.game.service.gameState.GameStateBroadcastService;
import com.server.game.service.troop.TroopManager;
import com.server.game.service.troop.TroopInstance;

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
    GameStateBroadcastService gameStateBroadcastService;
    PositionService positionService;
    TroopManager troopManager;
    ChampionService championService;
    
    // Store combat data for each game
    private final Map<String, Map<Short, CombatData>> combatDataMap = new ConcurrentHashMap<>();
    
    // Store attack cooldowns for each champion in each game
    private final Map<String, Map<Short, Long>> attackCooldowns = new ConcurrentHashMap<>();
    // Store skill cooldown for each champion in each game
    private final Map<String, Map<Short, Long>> skillCooldowns = new ConcurrentHashMap<>();
    
    /**
     * Check if a champion can attack (not on cooldown)
     */
    public boolean canChampionAttack(String gameId, short attackerSlot, ChampionEnum championEnum) {
        Map<Short, Long> gameCooldowns = attackCooldowns.get(gameId);
        if (gameCooldowns == null) {
            return true; // No cooldowns recorded, can attack
        }
        
        Long lastAttackTime = gameCooldowns.get(attackerSlot);
        if (lastAttackTime == null) {
            return true; // Never attacked, can attack
        }
        
        // Get champion's attack speed to calculate cooldown
        float attackCooldownSeconds = getChampionAttackCooldown(championEnum);
        long cooldownMillis = (long) (attackCooldownSeconds * 1000);
        
        long currentTime = System.currentTimeMillis();
        boolean canAttack = (currentTime - lastAttackTime) >= cooldownMillis;
        
        if (!canAttack) {
            long remainingCooldown = cooldownMillis - (currentTime - lastAttackTime);
            log.debug("Champion {} in slot {} still on cooldown for {}ms", 
                    championEnum, attackerSlot, remainingCooldown);
        }
        
        return canAttack;
    }
    
    /**
     * Set attack cooldown for a champion
     */
    private void setAttackCooldown(String gameId, short attackerSlot, long timestamp) {
        attackCooldowns.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                     .put(attackerSlot, timestamp);
    }
    
    /**
     * Get champion's attack cooldown in seconds (1 / attack_speed)
     */
    private float getChampionAttackCooldown(ChampionEnum championEnum) {
        Champion champion = championService.getChampionById(championEnum);
        if (champion == null) {
            log.warn("Champion not found: {}, using default cooldown", championEnum);
            return 1.0f; // Default 1 second cooldown
        }
        
        float attackSpeed = champion.getAttackSpeed();
        if (attackSpeed <= 0) {
            log.warn("Invalid attack speed {} for champion {}, using default", attackSpeed, championEnum);
            return 1.0f;
        }
        
        // Attack cooldown = 1 / attack_speed
        return 1.0f / attackSpeed;
    }
    
    /**
     * Clean up cooldowns when game ends
     */
    public void cleanupGameCooldowns(String gameId) {
        attackCooldowns.remove(gameId);
        log.debug("Cleaned up attack cooldowns for game: {}", gameId);
    }
    
    /**
     * Get remaining cooldown time for a champion in milliseconds
     */
    public long getRemainingCooldown(String gameId, short attackerSlot, ChampionEnum championEnum) {
        Map<Short, Long> gameCooldowns = attackCooldowns.get(gameId);
        if (gameCooldowns == null) {
            return 0; // No cooldowns recorded
        }
        
        Long lastAttackTime = gameCooldowns.get(attackerSlot);
        if (lastAttackTime == null) {
            return 0; // Never attacked
        }
        
        float attackCooldownSeconds = getChampionAttackCooldown(championEnum);
        long cooldownMillis = (long) (attackCooldownSeconds * 1000);
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastAttackTime;
        
        return Math.max(0, cooldownMillis - elapsed);
    }
    
    /**
     * Get attack cooldown duration for a champion in seconds
     */
    public float getChampionAttackCooldownDuration(ChampionEnum championEnum) {
        return getChampionAttackCooldown(championEnum);
    }
    
    /**
     * Handle champion attacking another champion
     */
    public void handleChampionAttackChampion(String gameId, short attackerSlot, short targetSlot, long timestamp) {
        log.info("Champion in slot {} attacking champion in slot {} in game {} at timestamp {}", 
                attackerSlot, targetSlot, gameId, timestamp);
        
        // Get attacker's champion info from slot mapping
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);
        ChampionEnum attackerChampion = slot2ChampionId != null ? slot2ChampionId.get(attackerSlot) : null;

        log.debug("DEBUG: AttackerChampion = {}, slot2ChampionId = {}", attackerChampion, slot2ChampionId);

        if (attackerChampion == null) {
            log.warn("No champion found for slot {} in game {}", attackerSlot, gameId);
            return;
        }
        
        // Check attack cooldown
        boolean canAttack = canChampionAttack(gameId, attackerSlot, attackerChampion);
        log.debug("DEBUG: Can champion attack? {}", canAttack);

        if (!canAttack) {
            log.debug("Champion {} in slot {} is on attack cooldown, ignoring attack request", 
                    attackerChampion, attackerSlot);
            return;
        }
        
        // Set attack cooldown
        setAttackCooldown(gameId, attackerSlot, timestamp);
        log.debug("DEBUG: Set attack cooldown, about to broadcast animation");
        
        // 1. First send attack animation of the attacker
        try {
            broadcastAttackerAnimation(gameId, attackerSlot, null, targetSlot, null, attackerChampion, timestamp, "Attack");
            log.debug("DEBUG: Successfully called broadcastAttackerAnimation");
        } catch (Exception e) {
            log.error("ERROR: Exception in broadcastAttackerAnimation", e);
        }
        
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
        
        // Check attack cooldown
        if (!canChampionAttack(gameId, attackerSlot, attackerChampion)) {
            log.debug("Champion {} in slot {} is on attack cooldown, ignoring attack request", 
                    attackerChampion, attackerSlot);
            return;
        }
        
        // Set attack cooldown
        setAttackCooldown(gameId, attackerSlot, timestamp);
        
        // 1. First send attack animation of the attacker
        broadcastAttackerAnimation(gameId, attackerSlot, null, (short) -1, targetId, attackerChampion, timestamp, "Attack");
        
        // 2. Process PvE attack and get damage
        int damage = processPvEAttack(gameId, targetId, timestamp);
        
        // 3. Send health update for the target
        broadcastHealthUpdateForTarget(gameId, targetId, damage, timestamp);
    }

    /**
     * Handle champion casting a skill
     */
    public void handleChampionSkillCast(String gameId, short casterSlot, Vector2 targetPosition, long timestamp) {
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);
        ChampionEnum casterChampion = slot2ChampionId != null ? slot2ChampionId.get(casterSlot) : null;

        if (casterChampion == null) {
            log.warn("No champion found for slot {} in game {}", casterSlot, gameId);
            return;
        }

        // Check if skill is on cooldown
        if (isSkillOnCooldown(gameId, casterSlot, casterChampion)) {
            log.debug("Champion {} in slot {} is on skill cooldown, ignoring skill cast request", 
                    casterChampion, casterSlot);
            return;
        }

        setSkillCooldown(gameId, casterSlot, casterChampion, timestamp);

        PositionData casterPosition = positionService.getPlayerPosition(gameId, casterSlot);   
        if (casterPosition == null) {
            log.warn("No position data found for champion in slot {} in game {}", casterSlot, gameId);
            return;
        }

        switch (casterChampion) {
            case MELEE_AXE -> handleAxeSkill(gameId, casterSlot, casterPosition.getPosition(), timestamp);
            case ASSASSIN_SWORD -> handleAssassinSkill(gameId, casterSlot, casterPosition.getPosition(), targetPosition, timestamp);
            case MARKSMAN_CROSSBOW -> handleArcherSkill(gameId, casterSlot, casterPosition.getPosition(), targetPosition, timestamp);
            case MAGE_SCEPTER -> handleWizardSkill(gameId, casterSlot, casterPosition.getPosition(), targetPosition, timestamp);
            default -> log.warn("Unknown champion type for skill: {}", casterChampion);
        }
    }
    
    /**
     * Handle target attacking a champion (PvE counter-attack)
     */
    public void handleTargetAttackChampion(String gameId, String attackerId, short defenderSlot, long timestamp) {
        log.info("Target {} attacking champion in slot {} in game {} at timestamp {}", 
                attackerId, defenderSlot, gameId, timestamp);

        // TODO: Implement target combat logic
    }
    
    /**
     * Handle target attacking another target
     */
    public void handleTargetAttackTarget(String gameId, String targetId, short slot, long timestamp) {
        log.info("Target attacking target {} in game {} at timestamp {}", 
                targetId, gameId, timestamp);

        // TODO: Implement target vs target combat logic
    }

    /**
     * Check if a skill is on cooldown
    */
    private boolean isSkillOnCooldown(String gameId, short casterSlot, ChampionEnum championType) {
        Map<Short, Long> gameCooldowns = skillCooldowns.get(gameId);
        if (gameCooldowns == null) {
            return false; // No cooldowns recorded, skill can be cast
        }

        Long lastSkillUseTime = gameCooldowns.get(casterSlot);
        if (lastSkillUseTime == null) {
            return false;
        }

        float skillCooldownSeconds = getChampionSkillCooldown(championType);
        long cooldownMillis = (long)(skillCooldownSeconds * 1000);

        long currentTime = System.currentTimeMillis();
        boolean isOnCooldown = (currentTime - lastSkillUseTime) < cooldownMillis;

        if (isOnCooldown) {
            long remainingCooldown = cooldownMillis - (currentTime - lastSkillUseTime);
            log.debug("Champion {} in slot {} is on skill cooldown for {}ms", 
                    championType, casterSlot, remainingCooldown);
        }

        return isOnCooldown;
    }

    /**
     * Set skill cooldown for a champion
     */
    private void setSkillCooldown(String gameId, short casterSlot, ChampionEnum championType, long timestamp) {
        skillCooldowns.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
            .put(casterSlot, timestamp);
        
        float cooldownSecs = getChampionSkillCooldown(championType);
        log.debug("Set skill cooldown for champion {} in slot {} to {} seconds",
                championType, casterSlot, cooldownSecs);
    }

    /**
     * Get champion's skill cooldown in seconds
     */
    private float getChampionSkillCooldown(ChampionEnum championType) {
        Champion champion = championService.getChampionById(championType);
        if (champion == null || champion.getAbility() == null) {
            log.warn("Champion not found: {}, using default skill cooldown", championType);
            return 5.0f; // Default 5 seconds cooldown
        }
        
        return champion.getAbility().getCooldown();
    }

    /**
     * Get remaining skill cooldown time for a champion in milliseconds
     * @param gameId
     * @param attackerSlot
     * @param attackerChampion
     * @param timestamp
     * @return
     */
    public long getRemainingSkillCooldown(String gameId, short casterSlot, ChampionEnum championType) {
        Map<Short, Long> gameCooldowns = skillCooldowns.get(gameId);
        if (gameCooldowns == null) {
            return 0; // No cooldowns recorded
        }

        Long lastSkillUseTime = gameCooldowns.get(casterSlot);
        if (lastSkillUseTime == null) {
            return 0; // Never used skill
        }

        float skillCooldownSeconds = getChampionSkillCooldown(championType);
        long cooldownMillis = (long)(skillCooldownSeconds * 1000);
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastSkillUseTime;
        
        return Math.max(0, cooldownMillis - elapsed);
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
        int damage = calculatePvEDamage(gameId, targetId);
        
        // TODO: Implement target health system, damage calculation, loot drops, etc.
        // For now, just return calculated damage
        
        return damage;
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
     * Calculate damage dealt to PvE targets (troops)
     */
    private int calculatePvEDamage(String gameId, String targetId) {
        // TODO: Implement target type based damage calculation
        // If target is a troop
        if (targetId.startsWith("troop_")) {
            TroopInstance troop = troopManager.getTroop(gameId, targetId);
            if (troop != null) {
                return calculateTroopDamageToPlayer(troop);
            } else {
                log.warn("Troop not found for target ID {}", targetId);
            }
        }
        return 1;
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
    private void broadcastAttackerAnimation(String gameId, short attackerSlot, String attackerId, short targetSlot, String targetId, ChampionEnum attackerChampion, long timestamp, String attackType) {
        log.info("DEBUG: broadcastAttackerAnimation called - gameId: {}, attackerSlot: {}, targetSlot: {}, champion: {}", 
                gameId, attackerSlot, targetSlot, attackerChampion);
        
        try {
            String animationType = attackType;
            log.info("DEBUG: Animation type: {}", animationType);
            
            // Create attack animation display message
            AttackAnimationDisplaySend attackAnimation = new AttackAnimationDisplaySend(
                    attackerSlot, attackerId, targetSlot, targetId, animationType, timestamp);
            
            log.info("DEBUG: Creating AttackAnimationDisplaySend with parameters - attackerSlot: {}, attackerId: {}, targetSlot: {}, targetId: {}, animationType: {}, timestamp: {}", 
                    attackerSlot, attackerId, targetSlot, targetId, animationType, timestamp);
            
            log.info("DEBUG: Successfully created AttackAnimationDisplaySend");
            
            // Get any channel from the game to trigger the framework
            Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
            log.info("DEBUG: Found {} channels for gameId {}", 
                    gameChannels != null ? gameChannels.size() : 0, gameId);

            if (gameChannels != null && !gameChannels.isEmpty()) {
                // Use any channel to trigger the framework - the AttackAnimationDisplaySend.getSendTarget() 
                // returns AMatchBroadcastTarget which will handle broadcasting to all channels
                Channel anyChannel = gameChannels.iterator().next();
                anyChannel.writeAndFlush(attackAnimation);
                log.info("DEBUG: Sent AttackAnimationDisplaySend to framework for broadcasting");
            } else {
                log.warn("DEBUG: No channels found for gameId {}", gameId);
            }
        } catch (Exception e) {
            log.error("DEBUG: Exception in broadcastAttackerAnimation: {}", e.getMessage(), e);
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
     * Handle Axe champion's skill
     */
    private void handleAxeSkill(String gameId, short casterSlot, Vector2 casterPosition, long timestamp) {
        log.info("Champion {} using Whirlwind skill at position {} in game {}",
                ChampionEnum.MELEE_AXE, casterPosition, gameId);

        broadcastAttackerAnimation(gameId, casterSlot, null, (short) -1, null, ChampionEnum.MELEE_AXE, timestamp, "Skill");
    }

    /** 
     * Handle Assassin champion's skill
     */
    public void handleAssassinSkill(String gameId, short casterSlot, Vector2 startPosition, Vector2 targetPosition, long timestamp) {
        log.info("Champion {} using Blink skill from {} to {} in game {}",
                ChampionEnum.ASSASSIN_SWORD, startPosition, targetPosition, gameId);
        // Broadcast attack animation
        broadcastAttackerAnimation(gameId, casterSlot, null, (short) -1, null, ChampionEnum.ASSASSIN_SWORD, timestamp, "Skill");
    }

    /**
     * Handle Archer champion's skill
     */
    private void handleArcherSkill(String gameId, short casterSlot, Vector2 startPosition, Vector2 targetPosition, long timestamp) {
        log.info("Champion {} using Piercing Arrow skill from {} to {}", casterSlot, startPosition, targetPosition);
        // Broadcast attack animation
        broadcastAttackerAnimation(gameId, casterSlot, null, (short) -1, null, ChampionEnum.MARKSMAN_CROSSBOW, timestamp, "Skill");
    }

    /**
     * Handle Wizard champion's skill
     */
    private void handleWizardSkill(String gameId, short casterSlot, Vector2 startPosition, Vector2 targetPosition, long timestamp) {
        log.info("Champion {} using Fireball skill from {} to {}", casterSlot, startPosition, targetPosition);
        // Broadcast attack animation
        broadcastAttackerAnimation(gameId, casterSlot, null, (short) -1, null, ChampionEnum.MAGE_SCEPTER, timestamp, "Skill");
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
