package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.deserializer.BurgDeserializer;
import com.server.game.resource.deserializer.TowerDeserializer;

import lombok.AccessLevel;



@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotInfo {
    short slot;
    @JsonProperty("spawn_position")
    Spawn spawn;
    Burg burg;
    List<Tower> towers;
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
    @JsonDeserialize(using = BurgDeserializer.class)
    public static class Burg {
        String id;
        Vector2 position;
        float width;
        float length;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonDeserialize(using = TowerDeserializer.class)
    public static class Tower {
        String id;
        Vector2 position;
        float width;
        float length;
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
    
    public Vector2 getRandomMinionPosition() {
        if (minionPositions != null && !minionPositions.isEmpty()) {
            int randomIndex = (int) (Math.random() * minionPositions.size());
            return minionPositions.get(randomIndex);
        }
        // Fallback to champion spawn position if no minion positions are defined
        return getSpawnPosition();
    }
}



