package com.server.game.resource.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

// {
//   "id": 3,
//   "name": "Wizard",
//   "role": "Mage",
//   "stats": {
//     "hp": 1500,
//     "defense": 40,
//     "attack": 40,
//     "move_speed": 6.5,
//     "attack_speed": 0.6,
//     "attack_range": 1.0,
//     "resource_claiming_speed": 4.0
//   },
//   "ability": {
//     "name": "Wizard's Skill name",
//     "cool_down": 10.0
//   }
// }
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Champion {

    Short id;
    String name;
    String role;
    ChampionStats stats;
    ChampionAbility ability;



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChampionStats {
        int hp;
        int defense;
        int attack;
        @JsonProperty("move_speed")
        float moveSpeed;
        @JsonProperty("attack_speed")
        float attackSpeed;
        @JsonProperty("attack_range")
        float attackRange;
        @JsonProperty("resource_claiming_speed")
        float resourceClaimingSpeed;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChampionAbility {
        String name;
        @JsonProperty("cool_down")
        float cooldown;
    }

    public Integer getInitialHP() {
        if (stats != null) {
            return stats.getHp();
        }
        System.out.println(">>> [Log in Champion] Initial HP not found for champion " + id);
        return null;
    }
}
