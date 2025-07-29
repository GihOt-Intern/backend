package com.server.game.mapper;


import org.mapstruct.Mapper;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.AttributeComponent;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.skillComponent.SkillFactory;
import com.server.game.resource.model.ChampionDB;

@Mapper(componentModel = "spring")
public interface ChampionMapper {

    default Champion toChampion(ChampionDB championDB){
        return new Champion(
            championDB.getId(),
            championDB.getName(),
            championDB.getRole(),
            new AttributeComponent(
                championDB.getStats().getDefense(),
                championDB.getStats().getAttack(),
                championDB.getStats().getMoveSpeed(),
                championDB.getStats().getAttackSpeed(),
                championDB.getStats().getAttackRange(),
                championDB.getStats().getResourceClaimingSpeed()
            ),
            new HealthComponent(championDB.getStats().getInitHP()),
            SkillFactory.createSkillFor(
                championDB.getId(), 
                championDB.getAbility()
            )
        );
    }
}