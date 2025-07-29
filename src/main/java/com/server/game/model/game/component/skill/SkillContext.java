package com.server.game.model.game.component.skill;

import java.util.Map;

import com.server.game.model.game.component.Entity;
import com.server.game.model.map.component.Vector2;

import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
public class SkillContext {
    private Vector2 targetPosition;
    private Entity targetEntity;
    private long currentTick;
    private Map<String, Object> extraData; // dùng cho skill custom
}