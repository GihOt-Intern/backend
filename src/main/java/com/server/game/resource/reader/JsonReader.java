package com.server.game.resource.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.game.resource.model.Champion;
import com.server.game.resource.model.GameMap;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.FileNotFoundException;
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
            InputStream is = getClass().getClassLoader().getResourceAsStream("map/" + mapName + ".json");
            if (is == null) {
                throw new FileNotFoundException("File not found: resources/map/" + mapName + ".json");
            }
            return objectMapper.readValue(is, GameMap.class);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Champion readChampionFromJson(String championName) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("champion/" + championName + ".json");
            if (is == null) {
                throw new FileNotFoundException("File not found: resources/champion/" + championName + ".json");
            }
            return objectMapper.readValue(is, Champion.class);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
