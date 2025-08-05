package com.server.game.model.game.component.attributeComponent;


import lombok.Getter;

@Getter
public class TroopAttributeComponent extends AttributeComponent {
    protected float detectionRange;
    protected Integer healingPower; // Optional for healers
    protected Float healingRange; // Optional for healers
    protected int cost;

    public TroopAttributeComponent(
        int defense, 
        float attackRange, 
        float detectionRange, 
        Integer healingPower, 
        Float healingRange, 
        int cost
    ) {
        super(defense);
        this.detectionRange = detectionRange;
        this.healingPower = healingPower;
        this.healingRange = healingRange;
        this.cost = cost;
    }
}
