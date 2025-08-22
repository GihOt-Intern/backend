package com.server.game.service.champion;

import com.server.game.resource.model.ChampionDB;
import com.server.game.resource.repository.ChampionDBRepository;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
@Slf4j
public class ChampionService {
    
    ChampionDBRepository championRepository;

    public ChampionDB getChampionDBById(ChampionEnum championEnum) {
        return championRepository.findById(championEnum.getChampionId()).orElseGet(() -> {
            log.info("Champion with id " + championEnum.getChampionId() + " not found.");
            return null;
        });
    }
}
