package com.server.game.model.game.attackStrategy;

import com.server.game.model.game.context.AttackContext;

public interface AttackStrategy {
    boolean performAttack(AttackContext ctx);
}