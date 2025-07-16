package com.server.game.resource.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.game.resource.model.Champion;
import com.server.game.resource.model.GameMap;

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

    public GameMap readMapFromJson(String mapName) {
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

    public Champion readChampionFromJson(String championName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("game/champion/" + championName + ".json");
            if (is == null) {
                // throw new FileNotFoundException("File not found: resources/game/champion/" + championName + ".json");
                System.out.println(">>> File not found: resources/game/champion/" + championName + ".json");
                return null;
            }
            return objectMapper.readValue(is, Champion.class);

        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println(">>> Error reading champion JSON: " + e.getMessage());
            return null;
        }
    }
}
