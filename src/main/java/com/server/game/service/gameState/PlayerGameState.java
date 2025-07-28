// package com.server.game.service.gameState;

// import com.server.game.util.ChampionEnum;

// import lombok.Getter;
// import lombok.Setter;
// import lombok.extern.slf4j.Slf4j;

// @Slf4j
// @Getter
// public class PlayerGameState {
    
//     private final short slot;
//     private final ChampionEnum championId;
//     private final int maxHP;
    
//     private int currentHP;
    
//     // Extended game state - can be expanded in the future
//     @Setter
//     private int troopCount = 0;
    
//     @Setter
//     private int gold = 0;
    
//     @Setter
//     private long lastSkillUseTime = 0;
    
//     @Setter
//     private float skillCooldownDuration = 0.0f;
    
//     @Setter
//     private boolean isDead = false;
    
//     @Setter
//     private long lastDamageTime = 0;
    
//     @Setter
//     private boolean isRegenerating = false;
    
//     // Additional extensible game state properties
//     @Setter
//     private int level = 1;
    
//     @Setter
//     private int experience = 0;
    
//     @Setter
//     private boolean isInvulnerable = false;
    
//     @Setter
//     private long invulnerabilityEndTime = 0;
    
//     @Setter
//     private int armorPoints = 0;
    
//     @Setter
//     private int magicResistance = 0;
    
//     @Setter
//     private long lastActionTime = 0;
    
//     public PlayerGameState(short slot, ChampionEnum championId, int maxHP) {
//         this.slot = slot;
//         this.championId = championId;
//         this.maxHP = maxHP;
//         this.currentHP = maxHP; // Start at full health
        
//         log.debug("Created PlayerGameState for slot: {}, champion: {}, maxHP: {}", 
//                 slot, championId, maxHP);
//     }
    
//     /**
//      * Apply damage to the player
//      */
//     public void takeDamage(int damage) {
//         if (damage <= 0) {
//             return;
//         }
        
//         int oldHP = this.currentHP;
//         this.currentHP = Math.max(0, this.currentHP - damage);
//         this.lastDamageTime = System.currentTimeMillis();
        
//         if (this.currentHP == 0 && oldHP > 0) {
//             this.isDead = true;
//             log.info("Player slot {} has died after taking {} damage", slot, damage);
//         }
        
//         log.debug("Player slot {} took {} damage: {} -> {}", slot, damage, oldHP, this.currentHP);
//     }
    
//     /**
//      * Heal the player
//      */
//     public void heal(int healAmount) {
//         if (healAmount <= 0 || this.isDead) {
//             return;
//         }
        
//         int oldHP = this.currentHP;
//         this.currentHP = Math.min(this.maxHP, this.currentHP + healAmount);
        
//         log.debug("Player slot {} healed for {} HP: {} -> {}", slot, healAmount, oldHP, this.currentHP);
//     }
    
//     /**
//      * Set current HP with bounds checking
//      */
//     public void setCurrentHP(int newHP) {
//         int oldHP = this.currentHP;
//         this.currentHP = Math.max(0, Math.min(newHP, this.maxHP));
        
//         // Update death status
//         if (this.currentHP == 0 && oldHP > 0) {
//             this.isDead = true;
//             log.info("Player slot {} has died", slot);
//         } else if (this.currentHP > 0 && this.isDead) {
//             this.isDead = false;
//             log.info("Player slot {} has been revived", slot);
//         }
//     }
    
//     /**
//      * Check if player is alive
//      */
//     public boolean isAlive() {
//         return this.currentHP > 0 && !this.isDead;
//     }
    
//     /**
//      * Get health percentage
//      */
//     public float getHealthPercentage() {
//         return (float) this.currentHP / this.maxHP;
//     }
    
//     /**
//      * Check if skill is on cooldown
//      */
//     public boolean isSkillOnCooldown() {
//         long currentTime = System.currentTimeMillis();
//         long timeSinceLastUse = currentTime - this.lastSkillUseTime;
//         return timeSinceLastUse < (this.skillCooldownDuration * 1000);
//     }
    
//     /**
//      * Use skill (puts it on cooldown)
//      */
//     public boolean useSkill() {
//         if (isSkillOnCooldown() || !isAlive()) {
//             return false;
//         }
        
//         this.lastSkillUseTime = System.currentTimeMillis();
//         log.debug("Player slot {} used skill, cooldown: {}s", slot, skillCooldownDuration);
//         return true;
//     }
    
//     /**
//      * Get remaining skill cooldown in seconds
//      */
//     public float getRemainingSkillCooldown() {
//         if (!isSkillOnCooldown()) {
//             return 0.0f;
//         }
        
//         long currentTime = System.currentTimeMillis();
//         long timeSinceLastUse = currentTime - this.lastSkillUseTime;
//         long cooldownMs = (long) (this.skillCooldownDuration * 1000);
//         return (cooldownMs - timeSinceLastUse) / 1000.0f;
//     }
    
//     /**
//      * Add gold to the player
//      */
//     public void addGold(int amount) {
//         if (amount > 0) {
//             this.gold += amount;
//             log.debug("Player slot {} earned {} gold, total: {}", slot, amount, this.gold);
//         }
//     }
    
//     /**
//      * Spend gold
//      */
//     public boolean spendGold(int amount) {
//         if (amount <= 0 || this.gold < amount) {
//             return false;
//         }
        
//         this.gold -= amount;
//         log.debug("Player slot {} spent {} gold, remaining: {}", slot, amount, this.gold);
//         return true;
//     }
    
//     /**
//      * Add troops
//      */
//     public void addTroops(int count) {
//         if (count > 0) {
//             this.troopCount += count;
//             log.debug("Player slot {} gained {} troops, total: {}", slot, count, this.troopCount);
//         }
//     }
    
//     /**
//      * Remove troops
//      */
//     public boolean removeTroops(int count) {
//         if (count <= 0 || this.troopCount < count) {
//             return false;
//         }
        
//         this.troopCount -= count;
//         log.debug("Player slot {} lost {} troops, remaining: {}", slot, count, this.troopCount);
//         return true;
//     }
    
//     /**
//      * Get time since last damage in milliseconds
//      */
//     public long getTimeSinceLastDamage() {
//         return System.currentTimeMillis() - this.lastDamageTime;
//     }
    
//     /**
//      * Check if player can start regenerating (no damage for X seconds)
//      */
//     public boolean canStartRegeneration(long noDemageCooldownMs) {
//         return getTimeSinceLastDamage() >= noDemageCooldownMs;
//     }
    
//     /**
//      * Add experience points and handle level up
//      */
//     public boolean addExperience(int expPoints) {
//         if (expPoints <= 0) {
//             return false;
//         }
        
//         this.experience += expPoints;
        
//         // Simple level calculation (100 EXP per level)
//         int newLevel = (this.experience / 100) + 1;
//         if (newLevel > this.level) {
//             this.level = newLevel;
//             log.info("Player slot {} leveled up to level {}", slot, this.level);
//             return true; // Level up occurred
//         }
        
//         return false; // No level up
//     }
    
//     /**
//      * Check if player is currently invulnerable
//      */
//     public boolean isCurrentlyInvulnerable() {
//         if (!this.isInvulnerable) {
//             return false;
//         }
        
//         long currentTime = System.currentTimeMillis();
//         if (currentTime >= this.invulnerabilityEndTime) {
//             this.isInvulnerable = false;
//             return false;
//         }
        
//         return true;
//     }
    
//     /**
//      * Apply invulnerability for a duration
//      */
//     public void applyInvulnerability(long durationMs) {
//         this.isInvulnerable = true;
//         this.invulnerabilityEndTime = System.currentTimeMillis() + durationMs;
//         log.debug("Player slot {} is now invulnerable for {}ms", slot, durationMs);
//     }
    
//     /**
//      * Apply damage with armor and magic resistance consideration
//      */
//     public void takeAdvancedDamage(int damage, boolean isMagicalDamage) {
//         if (damage <= 0 || !isAlive() || isCurrentlyInvulnerable()) {
//             return;
//         }
        
//         int resistance = isMagicalDamage ? this.magicResistance : this.armorPoints;
//         int actualDamage = Math.max(1, damage - resistance); // Minimum 1 damage
        
//         takeDamage(actualDamage);
        
//         log.debug("Player slot {} took {} damage (original: {}, resistance: {}, magical: {})", 
//                 slot, actualDamage, damage, resistance, isMagicalDamage);
//     }
    
//     /**
//      * Update last action time (for AFK detection)
//      */
//     public void updateLastActionTime() {
//         this.lastActionTime = System.currentTimeMillis();
//     }
    
//     /**
//      * Get time since last action in milliseconds
//      */
//     public long getTimeSinceLastAction() {
//         return System.currentTimeMillis() - this.lastActionTime;
//     }
    
//     /**
//      * Check if player is AFK (no action for specified time)
//      */
//     public boolean isAFK(long afkThresholdMs) {
//         return getTimeSinceLastAction() >= afkThresholdMs;
//     }
    
//     /**
//      * Get player status summary
//      */
//     public String getStatusSummary() {
//         return String.format("Slot %d (%s): Level %d, HP %d/%d, Gold: %d, Troops: %d, Alive: %s, AFK: %s",
//                 slot, championId, level, currentHP, maxHP, gold, troopCount, 
//                 isAlive(), isAFK(300000)); // 5 minute AFK threshold
//     }
// }
