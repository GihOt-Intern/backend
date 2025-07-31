package com.server.game.model.game.context;

import java.util.Map;

import org.springframework.lang.Nullable;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Delegate;


@AllArgsConstructor
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
    @Nullable
    private Map<String, Object> extraData; 
}