package com.server.game.model.game.skill;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.game.component.skillComponent.SkillContext;
import com.server.game.resource.model.ChampionDB.ChampionAbility;

public class MeleeSkill extends SkillComponent {

    public MeleeSkill(ChampionAbility ability) {
        super(ability);
    }


    @Override
    protected void doUse(Champion caster, SkillContext context) {
        // Implement the specific logic for using a Melee skill
        // This could involve dealing damage, applying effects, etc.
    }

    @Override
    public void update(long currentTick) {
        // Implement any periodic updates needed for the Melee skill
        // For example, if the skill has a duration or needs to check conditions over time
    }

}
