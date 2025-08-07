package com.server.game.model.game.championSkill;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.resource.model.ChampionDB.ChampionAbility;

public class MarksmanSkill extends SkillComponent {
    public MarksmanSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability);
    }

    @Override
    public boolean canUseWhileAttacking() {
        return false;
    }

    @Override
    protected void doUse() {
        // Implement the specific logic for using a Mage skill
        // This could involve dealing damage, applying effects, etc.
    }

    @Override
    protected boolean doUpdatePerTick() {
        // Implement any periodic updates needed for the Assassin skill
        // For example, if the skill has a duration or needs to check conditions over time
        return false;
    }
}
