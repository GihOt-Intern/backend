package com.server.game.model.game.component.attackComponent;


public interface AttackStrategy {
    void performAttack(AttackContext ctx);
}