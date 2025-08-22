package com.server.game.factory;

import org.springframework.stereotype.Component;

import com.server.game.model.game.Champion;
import com.server.game.model.game.championSkill.*;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.ChampionEnum;

@Component
public class SkillFactory {
    public SkillComponent createSkillFor(Champion owner, ChampionAbility ability) {
        ChampionEnum championEnum = owner.getChampionEnum();
        return switch (championEnum) {
            case MELEE_AXE                  -> new MeleeSkill(owner, ability);
            case ASSASSIN_SWORD             -> new AssassinSkill(owner, ability);
            case MARKSMAN_CROSSBOW          -> new ArcherSkill(owner, ability);
            case MAGE_SCEPTER               -> new MageSkill(owner, ability);
            default -> throw new IllegalArgumentException("Unknown champion: " + championEnum);
        };
    }
}
