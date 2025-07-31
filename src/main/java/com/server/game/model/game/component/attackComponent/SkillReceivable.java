package com.server.game.model.game.component.attackComponent;

import com.server.game.model.game.context.CastSkillContext;

public interface SkillReceivable {
    void receiveSkillDamage(CastSkillContext ctx);
}
