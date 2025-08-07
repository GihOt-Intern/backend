package com.server.game.model.game.championSkill;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.Util;

import lombok.extern.slf4j.Slf4j;
// Lướt thẳng tới trước 1 khoảng cách X, gây sát thương lên đối thủ trên đường đi
@Slf4j
public class AssassinSkill extends SkillComponent {

    private static final float DASH_LENGTH = 8.0f;
    private static final float DASH_WIDTH = 8.0f;

    public AssassinSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability);
    }

    private float getDamage() {
        return 20000f;
    }


    @Override
    public boolean canUseWhileAttacking() {
        return false;
    }

    @Override
    public boolean canUseWhileMoving() {
        return false;
    }

    @Override
    protected void doUse() {
        // long currentTick = this.getCastSkillContext().getCurrentTick();

        // this.startTick = currentTick;
        // this.endTick = startTick + Util.seconds2GameTick(DURATION_SECONDS);
    }
}
