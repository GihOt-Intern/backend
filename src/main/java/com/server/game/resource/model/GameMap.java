package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.sendObject.initialGameState.InitialPositionsSend.InitialPositionData;
import com.server.game.resource.deserializer.PlayGroundDeserializer;
import com.server.game.resource.model.SlotInfo.BurgDB;
import com.server.game.resource.model.SlotInfo.TowerDB;

import lombok.AccessLevel;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameMap {
    short id;
    String name;
    Integer initialGoldEachSlot;
    Integer goldGeneratedPerSecond;
    Integer towerHP;
    Integer burgHP;
    PlayGround playGround;
    List<SlotInfo> slotInfos;

    final Map<Short, SlotInfo> slot2SlotInfo = new HashMap<>();


    @JsonCreator
    public GameMap(
        @JsonProperty("id") short id,
        @JsonProperty("map_name") String name,
        @JsonProperty("initial_gold_each_slot") int initialGoldEachSlot,
        @JsonProperty("gold_generated_per_second") int goldGeneratedPerSecond,
        @JsonProperty("tower_hp") Integer towerHP,
        @JsonProperty("burg_hp") Integer burgHP,
        @JsonProperty("play_ground") PlayGround playGround,
        @JsonProperty("slot_info") List<SlotInfo> slotInfos
    ) {
        this.id = id;
        this.name = name;
        this.initialGoldEachSlot = initialGoldEachSlot;
        this.goldGeneratedPerSecond = goldGeneratedPerSecond;
        this.towerHP = towerHP;
        this.burgHP = burgHP;

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

    public Set<TowerDB> getTowers(short slot) {
        if (slot2SlotInfo.containsKey(slot)) {
            return slot2SlotInfo.get(slot).getTowersDB();
        }
        System.out.println(">>> [Log in GameMap] SlotInfo for slot " + slot + " not found.");
        return new HashSet<>();
    }

    public TowerDB getTowerDB(Short slot, String dbId) {
        return this.slot2SlotInfo.get(slot).getTowersDB().stream()
            .filter(towerDB -> towerDB.getId().equals(dbId))
            .findFirst()
            .orElse(null);
    }

    public BurgDB getBurgDB(Short slot) {
        if (slot2SlotInfo.containsKey(slot)) {
            return slot2SlotInfo.get(slot).getBurgDB();
        }
        System.out.println(">>> [Log in GameMap] SlotInfo for slot " + slot + " not found.");
        return null;
    }
}
