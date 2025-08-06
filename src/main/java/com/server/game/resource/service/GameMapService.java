package com.server.game.resource.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


import org.springframework.stereotype.Service;

import com.server.game.resource.model.GameMap;
import com.server.game.resource.repository.GameMapRepository;
import com.server.game.model.map.component.Vector2;

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


    public Vector2 getSpawnPosition(Short gameMapId, short slot) {
        GameMap gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            System.out.println(">>> [Log in GameMapService] GameMap with id " + gameMapId + " not found.");
            return null;
        }
        return gameMap.getSpawnPosition(slot);
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
