package com.server.game.resource.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.SlotInfo;
import com.server.game.resource.repository.mongo.GameMapRepository;
import com.server.game.map.component.Vector2;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend.InitialPositionData;

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

    public List<InitialPositionData> getInitialPositionsData(short gameMapId) {

        GameMap gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            return List.of();
        }   

        List<SlotInfo> slotInfos = gameMap.getSlotInfos();
        try {
            return slotInfos.stream() // Toi yeu phân sân nồ brồ gram ming
                .map(slotInfo -> {
                    return new InitialPositionData(slotInfo);
                })
                .collect(Collectors.toList());
       } catch (Exception e) {
            System.out.println(">>> [Log in GameMapService.getInitialPositionsData] Error with phân sân nồ brồ gram ming: " + e.getMessage());
            return List.of();
        }
    }

    public Vector2 getInitialPosition(Short gameMapId, short slot) {
        GameMap gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            System.out.println(">>> [Log in GameMapService] GameMap with id " + gameMapId + " not found.");
            return null;
        }
        return gameMap.getInitialPosition(slot);
    }


    public Float getInitialRotate(Short gameMapId, short slot) {
        GameMap gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            System.out.println(">>> [Log in GameMapService] GameMap with id " + gameMapId + " not found.");
            return null;
        }
        return gameMap.getInitialRotate(slot);
    }
}
