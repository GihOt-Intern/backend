package com.server.game.resource.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;

import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.SlotInfo;
import com.server.game.resource.repository.mongo.GameMapRepository;
import com.server.game.map.component.Vector2;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend.InitialPositionData;

import lombok.AccessLevel;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class GameMapService {

    GameMapRepository gameMapRepository;

    public GameMap getGameMapById(short id) {
        return gameMapRepository.findById(id).orElseGet(() -> {
            System.out.println(">>> [Log in GameMapService] GameMap with id " + id + " not found.");
            return null;
        });
    }

    public List<InitialPositionData> getInitialPositionsData(short gameMapId) {

        GameMap gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            return List.of();
        }   

        List<SlotInfo> slotInfos = gameMap.getSlotInfos();
        try {
            return slotInfos.stream() // Toi yeu phân sân nồ brồ gram ming
                .map(slotInfo -> {
                    return new InitialPositionData(slotInfo);
                })
                .collect(Collectors.toList());
       } catch (Exception e) {
            System.out.println(">>> [Log in GameMapService.getInitialPositionsData] Error with phân sân nồ brồ gram ming: " + e.getMessage());
            return List.of();
        }
    }

    public Coordinate getInitialPosition(Short gameMapId, short slot) {
        GameMap gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            System.out.println(">>> [Log in GameMapService] GameMap with id " + gameMapId + " not found.");
            return null;
        }
        return gameMap.getInitialPosition(slot);
    }


    public Float getInitialRotate(Short gameMapId, short slot) {
        GameMap gameMap = getGameMapById(gameMapId);
        if (gameMap == null) {
            System.out.println(">>> [Log in GameMapService] GameMap with id " + gameMapId + " not found.");
            return null;
        }
        return gameMap.getInitialRotate(slot);
    }


    // NavPolygon getNavPolygonById(Short gameMapId) {
    //     GameMap gameMap = getGameMapById(gameMapId);
    //     if (gameMap == null) {
    //         System.out.println(">>> [Log in GameMapService] GameMap with id " + gameMapId + " not found.");
    //         return null;
    //     }
    //     return new NavPolygon(2, gameMap.getBoundary(), null, true);
    // }


    // public Vector2 adjustTargetToMap(Vector2 startPosition, Vector2 targetPosition) {
    //     Short gameMapId = 2; // TODO: DO NOT HARDCODE THIS

    //     NavPolygon navBoundaryMap = this.getNavPolygonById(gameMapId);

    //     // TODO: contains but can move outside the map
    //     if (navBoundaryMap.contains(targetPosition)) {
    //         return targetPosition; // Target is within the map boundary
    //     }

    //     int n = navBoundaryMap.getNumVertices();
    //     for (int i = 0; i < n; i++) {
    //         Vector2 a = navBoundaryMap.getVertex(i);
    //         Vector2 b = navBoundaryMap.getVertex((i + 1) % n);

    //         Vector2 intersection = getLineSegmentIntersection(startPosition, targetPosition, a, b);
    //         if (intersection != null) {
    //             return intersection;
    //         }
    //     }

    //     // targetPosition is outside the map but does not intersect with any boundary edges
    //     // => ERROR
    //     System.out.println(">>> [Log in GameMapService] Target position " + targetPosition + " is outside the map boundary but does not intersect with any edges.");
    //     return null;
    // }
    

    // private Vector2 getLineSegmentIntersection(Vector2 p1, Vector2 p2, Vector2 q1, Vector2 q2) {
    //     float s1_x = p2.x() - p1.x();
    //     float s1_y = p2.y() - p1.y();
    //     float s2_x = q2.x() - q1.x();
    //     float s2_y = q2.y() - q1.y();

    //     float denom = (-s2_x * s1_y + s1_x * s2_y);
    //     if (denom == 0) { // song song hoặc trùng nhau
    //         System.out.println(">>> [Log in GameMapService] Line segments are parallel or coincident.");
    //         return null; // Không có giao điểm
    //     }

    //     float s = (-s1_y * (p1.x() - q1.x()) + s1_x * (p1.y() - q1.y())) / denom;
    //     float t = ( s2_x * (p1.y() - q1.y()) - s2_y * (p1.x() - q1.x())) / denom;

    //     if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
    //         // Có giao điểm
    //         return new Vector2(p1.x() + (t * s1_x), p1.y() + (t * s1_y));
    //     }

    //     return null; // Không giao nhau trong đoạn [p1-p2] và [q1-q2]
    // }
}
