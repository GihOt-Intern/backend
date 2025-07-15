package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.map.component.Vector2;
import com.server.game.resource.deserializer.MapObjectDeserializer;

import org.springframework.data.annotation.Id;

import lombok.AccessLevel;


@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameMap {
    @JsonIgnore
    @Id
    String id;
    @JsonProperty("mapName")
    String name;
    List<Vector2> boundary;
    @JsonProperty("spawn_positions")
    List<Spawn> spawnPositions;
    @JsonProperty("objects")
    List<MapObject> mapObjects;




    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonDeserialize(using = MapObjectDeserializer.class)
    public static class MapObject {
        String id;
        String type;
        Vector2 position;
        float width;
        float length;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Spawn {
        String id;
        Vector2 position;
    }
}
