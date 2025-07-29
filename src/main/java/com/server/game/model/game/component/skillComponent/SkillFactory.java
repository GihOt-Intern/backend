package com.server.game.model.game.component.skillComponent;

import com.server.game.model.game.skill.*;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.ChampionEnum;

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
