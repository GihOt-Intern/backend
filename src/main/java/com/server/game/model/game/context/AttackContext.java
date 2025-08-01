package com.server.game.model.game.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Delegate;


@Data
public class AttackContext { // only normal attacks, not skills
    
    @NotNull @Delegate
    private GameState gameState;
    @NotNull
    private Entity attacker;
    @NotNull
    private Entity target; 
    @NotNull
    private long timestamp;
    @NotNull
    private Map<Object, Object> extraData = new HashMap<>(); 

    public AttackContext(
        GameState gameState, Entity attacker, Entity target, long timestamp) {
        this.gameState = gameState;
        this.attacker = attacker;
        this.target = target;
        this.timestamp = timestamp;
    }

    public void addExtraData(Object key, Object value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value must not be null");
        }
        this.extraData.put(key, value);
    }

    public Integer getActualDamage() {
        Object value = this.extraData.get("actualDamage");
        if (value instanceof Integer int_val) {
            return int_val;
        }
        throw new IllegalArgumentException("Actual damage is not set in atkCtx extraData or is not an Integer");
    }
}