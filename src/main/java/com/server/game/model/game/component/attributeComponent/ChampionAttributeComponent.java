package com.server.game.model.game.component.attributeComponent;

import lombok.Getter;

@Getter
public class ChampionAttributeComponent extends AttributeComponent {
    protected float resourceClaimingSpeed;

    public ChampionAttributeComponent(int defense, float resourceClaimingSpeed) {
        super(defense);
        this.resourceClaimingSpeed = resourceClaimingSpeed;
    }
}
