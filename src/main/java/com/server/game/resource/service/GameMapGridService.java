package com.server.game.resource.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.server.game.resource.model.GameMapGrid;
import com.server.game.resource.model.GameMapGridCompress;
import com.server.game.resource.repository.GameMapGridCompressRepository;
import com.server.game.util.Util;

import lombok.AccessLevel;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class GameMapGridService {

    GameMapGridCompressRepository gameMapGridCompressRepository;

    
    private GameMapGridCompress compressGameMapGrid(GameMapGrid gameMapGrid) {
        List<String> gridCompressed = new ArrayList<>();
        for (boolean[] row : gameMapGrid.getGrid()) {
            gridCompressed.add(Util.compressBooleanArray(row));
        }

        return new GameMapGridCompress(
            gameMapGrid.getId(),
            gameMapGrid.getName(),
            gameMapGrid.getCornerA(),
            gameMapGrid.getCornerB(),
            gameMapGrid.getNRows(),
            gameMapGrid.getNCols(),
            gameMapGrid.getCellSize(),
            gridCompressed
        );
    }

    private GameMapGrid decompressGameMapGrid(GameMapGridCompress gameMapGridCompress) {
        Integer nRows = gameMapGridCompress.getNRows();
        Integer nCols = gameMapGridCompress.getNCols();
        boolean[][] grid = new boolean[nRows][nCols];
        for (int i = 0; i < nRows; i++) {
            grid[i] = Util.decompressBooleanArray(gameMapGridCompress.getGridCompressed().get(i), nCols);
        }

        return new GameMapGrid(
            gameMapGridCompress.getId(),
            gameMapGridCompress.getName(),
            gameMapGridCompress.getCornerA(),
            gameMapGridCompress.getCornerB(),
            gameMapGridCompress.getNRows(),
            gameMapGridCompress.getNCols(),
            gameMapGridCompress.getCellSize(),
            grid
        );
    }



    public void saveGameMapGrid(GameMapGrid gameMapGrid) {
        GameMapGridCompress gameMapGridCompress = compressGameMapGrid(gameMapGrid);
        gameMapGridCompressRepository.save(gameMapGridCompress);
    }

    public boolean existsById(short id) {
        return gameMapGridCompressRepository.existsById(id);
    }

    public boolean existsByName(String name) {
        return gameMapGridCompressRepository.existsByName(name);
    }

    public GameMapGrid getGameMapGridById(short id) {
        GameMapGridCompress gameMapGridCompress = gameMapGridCompressRepository.findById(id)
            .orElse(null);
        if (gameMapGridCompress == null) {
            System.out.println(">>> [Log in GameMapGridService] GameMapGridCompress with id " + id + " not found.");
            return null;
        }
        return decompressGameMapGrid(gameMapGridCompress);
    }

}
