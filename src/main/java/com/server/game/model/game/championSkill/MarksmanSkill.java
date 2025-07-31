package com.server.game.model.game.championSkill;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.resource.model.ChampionDB.ChampionAbility;

public class MarksmanSkill extends SkillComponent {
    public MarksmanSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability);
    }

    @Override
    protected void doUse(CastSkillContext context) {
        // Implement the specific logic for using a Mage skill
        // This could involve dealing damage, applying effects, etc.
    }

    @Override
    public void update(CastSkillContext context) {
        // Implement any periodic updates needed for the Assassin skill
        // For example, if the skill has a duration or needs to check conditions over time
    }
}
