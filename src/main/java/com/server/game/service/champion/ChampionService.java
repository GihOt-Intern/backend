package com.server.game.service.champion;

import com.server.game.mapper.ChampionMapper;
import com.server.game.model.gameState.Champion;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.initialGameState.ChampionInitialHPsSend.ChampionInitialHPData;
import com.server.game.resource.model.ChampionDB;
import com.server.game.resource.repository.ChampionDBRepository;
import com.server.game.resource.service.GameMapService;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ChampionService {
    
    GameMapService gameMapService;
    ChampionDBRepository championRepository;
    ChampionMapper championMapper;

    public Champion getChampionById(ChampionEnum championEnum) {
        ChampionDB championDB = getChampionDBById(championEnum);
        if (championDB == null) {
            return null;
        }
        return championMapper.toChampion(championDB);
    }

    

    private ChampionDB getChampionDBById(ChampionEnum championEnum) {
        return championRepository.findById(championEnum.getChampionId()).orElseGet(() -> {
            System.out.println(">>> [Log in ChampionService] Champion with id " + championEnum.getChampionId() + " not found.");
            return null;
        });
    }

    public Integer getInitialHP(ChampionEnum championId) {
        Champion champion = getChampionById(championId);
        if (champion == null) {
            System.out.println(">>> [Log in ChampionService] Champion with id " + championId + " not found.");
            return null;
        }
        return champion.getInitialHP();
    }

    public List<ChampionInitialHPData> getChampionInitialHPsData(String gameId) {
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);

        List<ChampionInitialHPData> initialHPDataList = new ArrayList<>();
        for(Map.Entry<Short, ChampionEnum> entry : slot2ChampionId.entrySet()) {
            Short slot = entry.getKey();
            ChampionEnum championId = entry.getValue();
            Integer initialHP = this.getInitialHP(championId);
            initialHPDataList.add(new ChampionInitialHPData(slot, initialHP));
        }
        return initialHPDataList;
    }
}
