package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.map.component.Vector2;
import com.server.game.resource.deserializer.GoldMineDeserializer;


import lombok.AccessLevel;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameMap {
    short id;
    String name;
    List<Vector2> boundary;
    GoldMine goldMine;
    List<SlotInfo> slotInfos;

    final Map<Short, SlotInfo> slotInfoMap = new HashMap<>();


    @JsonCreator
    public GameMap(
        @JsonProperty("id") short id,
        @JsonProperty("mapName") String name,
        @JsonProperty("boundary") List<Vector2> boundary,
        @JsonProperty("gold_mine") GoldMine goldMine,
        @JsonProperty("slot_info") List<SlotInfo> slotInfos
    ) {
        this.id = id;
        this.name = name;
        this.boundary = boundary;
        this.goldMine = goldMine;
        this.slotInfos = slotInfos;

        if (slotInfos != null) {
            for (SlotInfo slotInfo : slotInfos) {
                slotInfoMap.put(slotInfo.getSlot(), slotInfo);
            }
        }
    }


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


    public Vector2 getInitialPosition(short slot) {
        if (slotInfoMap.containsKey(slot)) {
            return slotInfoMap.get(slot).getInitialPosition();
        }
        System.out.println(">>> [Log in GameMap] SlotInfo for slot " + slot + " not found.");
        return null;
    }

    public Float getInitialRotate(short slot) {
        if (slotInfoMap.containsKey(slot)) {
            return slotInfoMap.get(slot).getInitialRotate();
        }
        System.out.println(">>> [Log in GameMap] SlotInfo for slot " + slot + " not found.");
        return null;
    }
}
