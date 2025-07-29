package com.server.game.model.game.component.skill;

import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.ChampionEnum;
import com.server.game.model.game.component.skill.concrete.*;

public class SkillFactory {
    public static SkillComponent createSkillFor(Short championId, ChampionAbility ability) {
        ChampionEnum championEnum = ChampionEnum.fromShort(championId);
        return switch (championEnum) {
            case MELEE_AXE                  -> new MeleeSkill(ability);
            case ASSASSIN_SWORD             -> new AssassinSkill(ability);
            case MARKSMAN_CROSSBOW          -> new MarksmanSkill(ability);
            case MAGE_SCEPTER               -> new MageSkill(ability);
            default -> throw new IllegalArgumentException("Unknown champion: " + championId);
        };
    }
}
