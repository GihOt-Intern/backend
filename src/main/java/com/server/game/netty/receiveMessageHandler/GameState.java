package com.server.game.netty.receiveMessageHandler;

import java.util.Map;

import org.locationtech.jts.geom.Polygon;

import com.server.game.map.MapWorld;
import com.server.game.resource.model.Champion;
import com.server.game.resource.model.GameMap;

import lombok.Data;

@Data
public class GameState {
    private MapWorld mapWorld;
    private Map<Short, Champion> champions;


    public GameState(GameMap gameMap, Map<Short, Champion> champions) {
        Short gameMapId = gameMap.getId();
        Polygon boundary = gameMap.getBoundary();
        float cellSize = 0.5f; // Default cell size, can be adjusted as needed
        // TODO: Adjust cell size as needed
        this.mapWorld = new MapWorld(gameMapId, boundary, cellSize);

        this.champions = champions;
    }
}