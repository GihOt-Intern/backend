package com.server.game.mapper;


import org.mapstruct.Mapper;

import com.server.game.map.object.Champion;
import com.server.game.map.object.Champion.AttributeComponent;
import com.server.game.map.object.abstraction.HealthComponent;
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
            championDB.getAbility(),
            new HealthComponent(championDB.getStats().getInitHP())
        );
    }
}