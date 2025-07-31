package com.server.game.model.game.component.attributeComponent;

import lombok.Getter;

@Getter
public class ChampionAttributeComponent extends AttributeComponent {
    protected float resourceClaimingSpeed;

    public ChampionAttributeComponent(int defense, float moveSpeed, float attackRange, float resourceClaimingSpeed) {
        super(defense, moveSpeed, attackRange);
        this.resourceClaimingSpeed = resourceClaimingSpeed;
    }
}
