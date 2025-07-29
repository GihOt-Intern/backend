package com.server.game.map.object;


import com.server.game.map.object.component.HealthComponent;
import com.server.game.resource.model.ChampionDB.ChampionAbility;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Champion {

    Short id;
    String name;
    String role;
    @Delegate
    AttributeComponent attributeComponent;
    @Delegate
    ChampionAbility ability;
    @Delegate
    HealthComponent healthComponent;
    

    @Getter
    @AllArgsConstructor
    public static class AttributeComponent {
        int defense;
        int attack;
        float moveSpeed;
        float attackSpeed;
        float attackRange;
        float resourceClaimingSpeed;
    }
}
