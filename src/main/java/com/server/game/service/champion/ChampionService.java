package com.server.game.service.champion;

import com.server.game.resource.model.ChampionDB;
import com.server.game.resource.repository.ChampionDBRepository;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ChampionService {
    
    ChampionDBRepository championRepository;

    public ChampionDB getChampionDBById(ChampionEnum championEnum) {
        return championRepository.findById(championEnum.getChampionId()).orElseGet(() -> {
            System.out.println(">>> [Log in ChampionService] Champion with id " + championEnum.getChampionId() + " not found.");
            return null;
        });
    }
}
