package com.server.game.resource.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.GameMap.SlotInfo;
import com.server.game.resource.model.GameMap.SlotInfo.Spawn;
import com.server.game.resource.repository.mongo.GameMapRepository;
import com.server.game.util.ChampionEnum;
import com.server.game.map.component.Vector2;
import com.server.game.netty.messageObject.sendObject.ChampionInitialPositionsSend.ChampionInitialPositionData;

import lombok.AccessLevel;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class GameMapService {

    GameMapRepository gameMapRepository;

    public GameMap getGameMapById(short id) {
        return gameMapRepository.findById(id).orElseGet(() -> {
            System.out.println(">>> [Log in GameMapService] GameMap with id " + id + " not found.");
            return null;
        });
    }



    public List<ChampionInitialPositionData> getChampionPositionsData(
        short gameMapId, 
        Map<Short, ChampionEnum> slot2ChampionId) {
        
        GameMap gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            return List.of();
        }

        List<SlotInfo> slotInfos = gameMap.getSlotInfo();

        try {
            return slotInfos.stream() // Toi yeu phân sân nồ brồ gram ming
                .map(slotInfo -> {
                    Short slot = slotInfo.getSlot();
                    Spawn spawn = slotInfo.getSpawn();
                    Vector2 position = spawn.getPosition();
                    float rotate = spawn.getRotate();
                    ChampionEnum championId = slot2ChampionId.get(slot);
                    return new ChampionInitialPositionData(slot, championId, position, rotate);
                })
                .collect(Collectors.toList());
       } catch (Exception e) {
            System.out.println(">>> [Log in GameMapService.getChampionPositionsData] Error with phân sân nồ brồ gram ming: " + e.getMessage());
            return List.of();
        }
    }

}
