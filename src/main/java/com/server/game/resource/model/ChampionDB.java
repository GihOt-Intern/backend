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
//     "gold_mine_damage": 2
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
public class ChampionDB {

    Short id;
    String name;
    String role;
    ChampionDBStats stats;
    ChampionAbility ability;


    // *********** INNER CLASSES ***********
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChampionDBStats {
        @JsonProperty("hp")
        int initHP;
        int defense;
        int attack;
        @JsonProperty("move_speed")
        float moveSpeed;
        @JsonProperty("attack_speed")
        float attackSpeed;
        @JsonProperty("attack_range")
        float attackRange;
        @JsonProperty("gold_mine_damage")
        int goldMineDamage;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChampionAbility {
        String name;
        @JsonProperty("cool_down")
        float cooldown;
    }
    // *********** END OF INNER CLASSES ***********
}
