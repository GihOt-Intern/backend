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
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.deserializer.PlayGroundDeserializer;

import lombok.AccessLevel;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameMap {
    short id;
    String name;
    Integer initialGoldEachSlot;
    Integer goldGeneratedPerSecond;
    PlayGround playGround;
    List<SlotInfo> slotInfos;

    final Map<Short, SlotInfo> slot2SlotInfo = new HashMap<>();


    @JsonCreator
    public GameMap(
        @JsonProperty("id") short id,
        @JsonProperty("map_name") String name,
        @JsonProperty("initial_gold_each_slot") int initialGoldEachSlot,
        @JsonProperty("gold_generated_per_second") int goldGeneratedPerSecond,
        @JsonProperty("play_ground") PlayGround playGround,
        @JsonProperty("slot_info") List<SlotInfo> slotInfos
    ) {
        this.id = id;
        this.name = name;
        this.initialGoldEachSlot = initialGoldEachSlot;
        this.goldGeneratedPerSecond = goldGeneratedPerSecond;
        this.playGround = playGround;
        this.slotInfos = slotInfos;

        if (slotInfos != null) {
            for (SlotInfo slotInfo : slotInfos) {
                slot2SlotInfo.put(slotInfo.getSlot(), slotInfo);
            }
        }
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonDeserialize(using = PlayGroundDeserializer.class)
    public static class PlayGround {
        String id;
        Vector2 position;
        float width;
        float length;

        public boolean isInGoldMine(Vector2 position) {
            return position.isInRectangle(this.position, this.width, this.length);
        }
    }


    public Vector2 getSpawnPosition(short slot) {
        if (slot2SlotInfo.containsKey(slot)) {
            return slot2SlotInfo.get(slot).getSpawnPosition();
        }
        System.out.println(">>> [Log in GameMap] SlotInfo for slot " + slot + " not found.");
        return null;
    }

    public Float getInitialRotate(short slot) {
        if (slot2SlotInfo.containsKey(slot)) {
            return slot2SlotInfo.get(slot).getInitialRotate();
        }
        System.out.println(">>> [Log in GameMap] SlotInfo for slot " + slot + " not found.");
        return null;
    }
}
