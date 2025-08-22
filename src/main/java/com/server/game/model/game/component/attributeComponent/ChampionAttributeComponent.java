package com.server.game.model.game.component.attributeComponent;

import lombok.Getter;

@Getter
public class ChampionAttributeComponent extends AttributeComponent {
    protected int goldMineDamage;

    public ChampionAttributeComponent(int defense, int goldMineDamage) {
        super(defense);
        this.goldMineDamage = goldMineDamage;
    }
}
