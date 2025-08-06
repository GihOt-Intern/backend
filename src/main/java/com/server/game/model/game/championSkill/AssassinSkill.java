package com.server.game.model.game.championSkill;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.resource.model.ChampionDB.ChampionAbility;

public class AssassinSkill extends SkillComponent {

    public AssassinSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability);
    }

    @Override
    protected void doUse() {
        // Implement the specific logic for using an Assassin skill
        // This could involve dealing damage, applying effects, etc.
    }

    @Override
    public boolean updatePerTick() {
        // Implement any periodic updates needed for the Assassin skill
        // For example, if the skill has a duration or needs to check conditions over time
        return false;
    }

}
