package com.server.game.model.game.attackStrategy;

import com.server.game.model.game.context.AttackContext;

public interface AttackStrategy {
    void performAttack(AttackContext ctx);
}