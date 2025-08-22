package com.server.game.model.game.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;

import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.entityIface.SkillReceivable;
import com.server.game.model.map.component.Vector2;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Delegate;


@AllArgsConstructor
@Data
@EqualsAndHashCode(exclude = "caster")
public class CastSkillContext {
    @NotNull @Delegate
    private GameState gameState;
    @NotNull
    private Champion caster;
    @Nullable
    private Vector2 targetPoint; // Client's mouse point when casting the skill
    @NotNull
    private long timestamp; 
    @Nullable
    private SkillReceivable target = null; // Can be null if the skill does not target an entity
    @NotNull
    private Float skillLength = 0.0f; // Length of the skill cast, can be 0 if not applicable

    private Map<Object, Object> extraData = new HashMap<>(); // Store other data if needed (damage, heal,...)

    public CastSkillContext(
        GameState gameState, Champion caster, Vector2 targetPoint, long timestamp) {
        this.gameState = gameState;
        this.caster = caster;
        this.targetPoint = targetPoint;
        this.timestamp = timestamp;
    }
    
    public CastSkillContext(
        GameState gameState, Champion caster, SkillReceivable target, Vector2 targetPoint, long timestamp) {
        this.gameState = gameState;
        this.caster = caster;
        this.target = target;
        this.targetPoint = targetPoint;
        this.timestamp = timestamp;
    }

    public void addExtraData(Object key, Object value) {
        if (extraData == null) {
            extraData = new HashMap<>();
        }
        extraData.put(key, value);
    }

    public void addSkillDamage(Float damage) {
        if (damage == null) {
            throw new IllegalArgumentException("Damage must not be null");
        }
        this.addExtraData("skillDamage", damage);
    }

    public Float getSkillDamage() {
        Object value = extraData.get("skillDamage");
        if (value == null) {
            throw new IllegalArgumentException("Skill damage is not set in CastSkillContext");
        }
        if (value instanceof Float float_val) {
            return float_val;
        }
        throw new IllegalArgumentException("Skill damage is not set in CastSkillContext or is not a Float");
    }

    public void addActualDamage(Integer actualDamage) {
        if (actualDamage == null) {
            throw new IllegalArgumentException("Actual damage must not be null");
        }
        this.addExtraData("actualDamage", actualDamage);
    }

    public Integer getActualDamage() {
        Object value = extraData.get("actualDamage");
        if (value == null) {
            throw new IllegalArgumentException("Actual damage is not set in CastSkillContext");
        }
        if (value instanceof Integer int_val) {
            return int_val;
        }
        throw new IllegalArgumentException("Actual damage is not set in CastSkillContext or is not an Integer");
    }
}