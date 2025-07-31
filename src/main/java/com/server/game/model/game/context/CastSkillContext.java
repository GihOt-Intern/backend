package com.server.game.model.game.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.map.component.Vector2;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Delegate;


@AllArgsConstructor
@Data
public class CastSkillContext {
    @NotNull @Delegate
    private GameState gameState;
    @NotNull
    private Entity caster;
    @Nullable
    private Vector2 targetPoint; // Client's mouse point when casting the skill
    @Nullable
    private Entity target; // Can be null if the skill does not target an entity
    @Nullable
    private Map<Object, Object> extraData; // Store other data if needed (damage, heal,...)

    @SuppressWarnings("null")
    public void addExtraData(Object key, Object value) {
        if (extraData == null) {
            extraData = new HashMap<>();
        }
        extraData.put(key, value);
    }


}