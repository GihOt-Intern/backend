package com.server.game.model.game;

import com.server.game.util.TroopEnum;

import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
public class TroopCreateContext {
    private TroopEnum troopEnum;
    private short ownerSlot;
    private GameState gameState; 
    private String gameId;
}