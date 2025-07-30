package com.server.game.resource.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.ChampionDB;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.resource.model.Troop;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;


@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class JsonReader {

    ObjectMapper objectMapper;

    public GameMap readGameMapFromJson(String mapName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("game/map/" + mapName + ".json");
            if (is == null) {
                // throw new FileNotFoundException("File not found: resources/game/map/" + mapName + ".json");
                System.out.println(">>> File not found: resources/game/map/" + mapName + ".json");
                return null;
            }
            return objectMapper.readValue(is, GameMap.class);

        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println(">>> Error reading map JSON: " + e.getMessage());
            return null;
        }
    }

    public GameMapGrid readGameMapGridFromJson(String mapName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("game/map/" + mapName + "_grid.json");
            if (is == null) {
                // throw new FileNotFoundException("File not found: resources/game/map/" + mapName + "_grid.json");
                System.out.println(">>> File not found: resources/game/map/" + mapName + "_grid.json");
                return null;
            }
            return objectMapper.readValue(is, GameMapGrid.class);

        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println(">>> Error reading map grid JSON: " + e.getMessage());
            return null;
        }
    }   

    public ChampionDB readChampionFromJson(String championName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("game/champion/" + championName + ".json");
            if (is == null) {
                // throw new FileNotFoundException("File not found: resources/game/champion/" + championName + ".json");
                System.out.println(">>> File not found: resources/game/champion/" + championName + ".json");
                return null;
            }
            return objectMapper.readValue(is, ChampionDB.class);

        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println(">>> Error reading champion JSON: " + e.getMessage());
            return null;
        }
    }
    
    public Troop readTroopFromJson(String troopName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("game/troop/" + troopName + ".json");
            if (is == null) {
                System.out.println(">>> File not found: resources/game/troop/" + troopName + ".json");
                return null;
            }
            return objectMapper.readValue(is, Troop.class);
            
        } catch (IOException e) {
            System.out.println(">>> Error reading troop JSON: " + e.getMessage());
            return null;
        }
    }
}
