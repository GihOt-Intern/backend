package com.server.game.model.game.component.attackComponent;

import com.server.game.model.game.context.AttackContext;

public interface Attackable {
    void receiveAttack(AttackContext ctx);
}
