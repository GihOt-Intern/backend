package com.server.game.model.game.championSkill;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.resource.model.ChampionDB.ChampionAbility;



public class MageSkill extends SkillComponent {

    public MageSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability);
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
    protected boolean doUse() {
        // Implement the specific logic for using a Mage skill
        // This could involve dealing damage, applying effects, etc.
    
        return true; // Indicate that the skill was used successfully
    }
}
