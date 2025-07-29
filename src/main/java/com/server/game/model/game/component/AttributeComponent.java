package com.server.game.model.game.component;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttributeComponent {
    int defense;
    int attack;
    float moveSpeed;
    float attackSpeed;
    float attackRange;
    float resourceClaimingSpeed;
}
