package com.server.game.resource.writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.ChampionDB;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.resource.model.TroopDB;
import com.server.game.resource.reader.JsonReader;
import com.server.game.resource.repository.ChampionDBRepository;
import com.server.game.resource.repository.GameMapRepository;
import com.server.game.resource.repository.TroopDBRepository;
import com.server.game.resource.service.GameMapGridService;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class Writer {

    JsonReader jsonReader;
    GameMapRepository mapRepository;
    ChampionDBRepository championDBRepository;
    TroopDBRepository troopDBRepository;

    GameMapGridService gameMapGridService;


    private final List<String> mapNames = new ArrayList<>(Arrays.asList(
        "map_2"
    ));

    private final List<String> championNames = new ArrayList<>(Arrays.asList(
        "Axe",
        "Knight",
        "Archer",
        "Wizard"
    ));
    
    private final List<String> troopNames = new ArrayList<>(Arrays.asList(
        "Axis",
        "Crossbawl",
        "Healer",
        "Shadow"
    ));
    

    @EventListener(ApplicationReadyEvent.class)
    public void writeMaps() {
        for (String mapName : mapNames) {
            if (mapRepository.existsByName(mapName)
                && false
             ) { // remove && false when not debugging
                System.out.println("Map already exists: " + mapName);
                continue;
            }
            this.writeMap(mapName); 
        }
    }


    private void writeMap(String mapName) {
        GameMap map = jsonReader.readGameMapFromJson(mapName);
        if (map == null) {
            System.out.println("Failed to read map from JSON.");
            return;
        }
        mapRepository.save(map);
        System.out.println("Map saved successfully: " + map.getName());
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void writeMapGrids() {
        for (String mapName : mapNames) {
            if (gameMapGridService.existsByName(mapName)
                && false
             ) { // remove && false when not debugging
                System.out.println("Game map already exists: " + mapName);
                continue;
            }
            this.writeGameMapGrid(mapName);
        }
    }


    private void writeGameMapGrid(String mapName) {
        GameMapGrid mapGrid = jsonReader.readGameMapGridFromJson(mapName);
        if (mapGrid == null) {
            System.out.println("Failed to read map grid from JSON.");
            return;
        }
        gameMapGridService.saveGameMapGrid(mapGrid);
        System.out.println("Game map grid saved successfully: " + mapGrid.getId());
    }


    @EventListener(ApplicationReadyEvent.class)
    public void writeChampions() {
        for (String championName : championNames) {
            if (championDBRepository.existsByName(championName) 
                && false
            ) { // remove && false when not debugging
                System.out.println("Champion already exists: " + championName);
                continue;
            }
            this.writeChampion(championName.toLowerCase());
        }
    }

    private void writeChampion(String championName) {
        ChampionDB champion = jsonReader.readChampionFromJson(championName);
        if (champion == null) {
            System.out.println("Failed to read champion from JSON.");
            return;
        }
        championDBRepository.save(champion);
        System.out.println("Champion saved successfully: " + champion.getName());
    }


    @EventListener(ApplicationReadyEvent.class)
    public void writeTroops() {
        for (String troopName : troopNames) {
            if (troopDBRepository.existsByName(troopName) 
                && false
            ) { // remove && false when not debugging
                System.out.println("Troop already exists: " + troopName);
                continue;
            }
            this.writeTroop(troopName.toLowerCase());
        }
    }

    private void writeTroop(String troopName) {
        TroopDB troop = jsonReader.readTroopFromJson(troopName);
        if (troop == null) {
            System.out.println("Failed to read troop from JSON.");
            return;
        }
        troopDBRepository.save(troop);
        System.out.println("Troop saved successfully: " + troop.getName());
    }
}
