package com.server.game.model.game.component.skill.concrete;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.skill.SkillComponent;
import com.server.game.model.game.component.skill.SkillContext;
import com.server.game.resource.model.ChampionDB.ChampionAbility;

public class MarksmanSkill extends SkillComponent {
    public MarksmanSkill(ChampionAbility ability) {
        super(ability);
    }

    @Override
    protected void doUse(Champion caster, SkillContext context) {
        // Implement the specific logic for using a Mage skill
        // This could involve dealing damage, applying effects, etc.
    }

    @Override
    public void update(long currentTick) {
        // Implement any periodic updates needed for the Assassin skill
        // For example, if the skill has a duration or needs to check conditions over time
    }
}
