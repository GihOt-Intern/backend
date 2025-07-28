package com.server.game.resource.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Delegate;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Troop {

    Short id;
    String name;
    String type;
    String role;
    @Delegate
    TroopStats stats;
    @Delegate
    TroopAbility ability;
    @Delegate
    TroopAIBehavior ai_behavior;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TroopStats {
        int hp;
        int defense;
        int attack;
        @JsonProperty("move_speed")
        float moveSpeed;
        @JsonProperty("attack_speed")
        float attackSpeed;
        @JsonProperty("attack_range")
        float attackRange;
        @JsonProperty("detection_range")
        float detectionRange;
        @JsonProperty("healing_power")
        Integer healingPower; // Optional for healers
        @JsonProperty("healing_range")
        Float healingRange; // Optional for healers
        int cost;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TroopAbility {
        String name;
        String description;
        @JsonProperty("cool_down")
        float coolDown;
        TroopAbilityEffect effect;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TroopAbilityEffect {
        String type;
        @JsonProperty("value")
        Float value; // Optional
        @JsonProperty("damage_multiplier")
        Float damageMultiplier; // Optional
        @JsonProperty("duration")
        Float duration; // Optional
        @JsonProperty("stealth_duration")
        Float stealthDuration; // Optional
        @JsonProperty("heal_amount")
        Integer healAmount; // Optional
        @JsonProperty("heal_radius")
        Float healRadius; // Optional
        @JsonProperty("max_targets")
        Integer maxTargets; // Optional
        @JsonProperty("pierce_range")
        Float pierceRange; // Optional
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TroopAIBehavior {
        float aggression;
        @JsonProperty("pursuit_range")
        float pursuitRange;
        @JsonProperty("retreat_health_threshold")
        float retreatHealthThreshold;
        @JsonProperty("group_behavior")
        String groupBehavior;
        @JsonProperty("target_priority")
        String targetPriority;
        @JsonProperty("kiting_behavior")
        Boolean kitingBehavior; // Optional
        @JsonProperty("healing_threshold")
        Float healingThreshold; // Optional for healers
        @JsonProperty("stay_behind_frontline")
        Boolean stayBehindFrontline; // Optional for support units
    }

    // Helper methods
    public Integer getInitialHP() {
        if (stats != null) {
            return stats.getHp();
        }
        System.out.println(">>> [Log in Troop] Initial HP not found for troop " + id);
        return null;
    }

    public Float getAttackRange() {
        if (stats != null) {
            return stats.getAttackRange();
        }
        return 1.0f; // Default attack range
    }

    public Float getDetectionRange() {
        if (stats != null) {
            return stats.getDetectionRange();
        }
        return 3.0f; // Default detection range
    }

    public boolean isHealer() {
        return "support".equalsIgnoreCase(type) && stats != null && stats.getHealingPower() != null;
    }

    public boolean isRanged() {
        return "ranged".equalsIgnoreCase(type);
    }

    public boolean isMelee() {
        return "melee".equalsIgnoreCase(type) || "assassin".equalsIgnoreCase(type);
    }
}
