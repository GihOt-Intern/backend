package com.server.game.model.game.component.attackComponent;

import java.util.Map;

import com.server.game.model.game.Entity;
import com.server.game.model.map.component.Vector2;

import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
public class AttackContext {
    private String gameId;
    
    private short attackerSlot; // null if attacker is not Champion
    private String attackerId;  // null if attackerSlot is not null
    
    private short targetSlot;   // null if target is not Champion
    private String targetId;    // null if targetSlot is not null
    
    private Entity attacker;

    private Vector2 targetPosition;
    private Entity target;
    
    private long currentTick;
    private long timestamp;

    private Map<String, Object> extraData; 
}