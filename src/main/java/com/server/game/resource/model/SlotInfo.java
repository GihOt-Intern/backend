package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.deserializer.BurgDBDeserializer;
import com.server.game.resource.deserializer.TowerDBDeserializer;

import lombok.AccessLevel;



@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotInfo {
    short slot;
    @JsonProperty("spawn_position")
    Spawn spawn;
    @JsonProperty("burg")
    BurgDB burgDB;
    @JsonProperty("towers")
    Set<TowerDB> towersDB;
    @JsonProperty("minion_positions")
    List<Vector2> minionPositions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Spawn {
        String id;
        Vector2 position;
        float rotate;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonDeserialize(using = BurgDBDeserializer.class)
    public static class BurgDB {
        String id;
        Vector2 position;
        float width;
        float length;
        float rotate; 
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonDeserialize(using = TowerDBDeserializer.class)
    public static class TowerDB {
        String id;
        Vector2 position;
        float width;
        float length;
        float rotate; 
    }        

    public Vector2 getSpawnPosition() {
        if (spawn != null) {
            return spawn.getPosition();
        }
        return null;
    }

    public Float getInitialRotate() {
        if (spawn != null) {
            return spawn.getRotate();
        }
        return null;
    }
}



