package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.map.MapWorld;
import com.server.game.map.component.Vector2;
import com.server.game.resource.deserializer.GoldMineDeserializer;


import lombok.AccessLevel;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameMap {
    short id;
    String name;
    Polygon boundary;
    GoldMine goldMine;
    List<SlotInfo> slotInfos;

    final Map<Short, SlotInfo> slotInfoMap = new HashMap<>();


    @JsonCreator
    public GameMap(
        @JsonProperty("id") short id,
        @JsonProperty("mapName") String name,
        @JsonProperty("boundary") List<Coordinate> boundary_vertices,
        @JsonProperty("gold_mine") GoldMine goldMine,
        @JsonProperty("slot_info") List<SlotInfo> slotInfos
    ) {
        this.id = id;
        this.name = name;
        this.boundary = GameMap.createPolygon(boundary_vertices);
        this.goldMine = goldMine;
        this.slotInfos = slotInfos;

        if (slotInfos != null) {
            for (SlotInfo slotInfo : slotInfos) {
                slotInfoMap.put(slotInfo.getSlot(), slotInfo);
            }
        }
    }

    public static Polygon createPolygon(List<Coordinate> vertices) {
        if (vertices == null || vertices.size() < 3) {
            throw new IllegalArgumentException("Polygon must have at least 3 coordinates");
        }

        // Đảm bảo danh sách có thể thay đổi được
        if (!vertices.get(0).equals2D(vertices.get(vertices.size() - 1))) {
            vertices = new ArrayList<>(vertices); // avoid mutating original list
            vertices.add(new Coordinate(vertices.get(0).x, vertices.get(0).y));
        }

        Coordinate[] coords = vertices.toArray(new Coordinate[0]);
        GeometryFactory factory = new GeometryFactory();
        LinearRing shell = factory.createLinearRing(coords);
        return factory.createPolygon(shell);
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonDeserialize(using = GoldMineDeserializer.class)
    public static class GoldMine {
        String id;
        Coordinate position;
        float width;
        float length;
    }


    public Coordinate getInitialPosition(short slot) {
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
