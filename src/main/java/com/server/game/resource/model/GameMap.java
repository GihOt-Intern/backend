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
import com.server.game.resource.deserializer.BurgDeserializer;
import com.server.game.resource.deserializer.GoldMineDeserializer;
import com.server.game.resource.deserializer.TowerDeserializer;

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
    @JsonProperty("gold_mine")
    GoldMine goldMine;
    @JsonProperty("slot_info")
    List<SlotInfo> slotInfo;




    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonDeserialize(using = GoldMineDeserializer.class)
    public static class GoldMine {
        String id;
        Vector2 position;
        float width;
        float length;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SlotInfo {
        short slot;
        Spawn spawn;
        Burg burg;
        List<Tower> towers;

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
    }


}
