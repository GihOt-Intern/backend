package com.server.game.model.game.component.attackComponent;

import java.util.Map;

import com.server.game.model.game.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
public class AttackedContext {
    private Entity attacker;
    private int damage;
    private long currentTick;
    private Map<String, Object> extraData; // dùng cho skill custom


    public AttackedContext(Entity attacker, AttackContext attCtx) {
        this.attacker = attacker;
        this.damage = attacker.getComponent(AttackComponent.class).getDamage();
        this.currentTick = attCtx.getCurrentTick();
        this.extraData = attCtx.getExtraData();
    }
}