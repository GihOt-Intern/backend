package com.server.game.model.game.championSkill;

import java.util.Set;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.attackComponent.SkillReceiver;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.Util;

import lombok.extern.slf4j.Slf4j;

import com.server.game.model.map.shape.CircleShape;

// Xoay rìu trong 5s, mỗi giây gây sát thương phạm vi xung quanh 1 ô = 40 + 20% DEF		
@Slf4j
public class MeleeSkill extends SkillComponent {

    private static final float DURATION_SECONDS = 5.0f;
    private static final float DAMAGE_INTERVAL_SECONDS = 1.0f;
    private static final float DAMAGE_RADIUS = 5.0f;


    private long startTick = -1;                // Khi bắt đầu kích hoạt skill
    private long endTick = -1;                  // Khi kết thúc
    private long nextDamageTick = -1;           // Tick tiếp theo cần gây damage


    public float getDamagePerSecond() {
        // return 40 + 0.2f * this.getSkillOwner().getDefense();

        return 20000f;
    }

    public MeleeSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability);
    }

    @Override
    protected void doUse() {
        long currentTick = this.getCastSkillContext().getCurrentTick();

        this.startTick = currentTick;
        this.endTick = startTick + Util.seconds2GameTick(DURATION_SECONDS);
        this.nextDamageTick = this.startTick; // Get damage immediately
    }

    @Override
    public boolean updatePerTick() {
        if (!this.isActive) { return false; }

        long currentTick = this.getCastSkillContext().getCurrentTick();

        // Kết thúc skill
        if (currentTick >= endTick) {
            log.info("MeleeSkill ended for champion: {}", this.getSkillOwner().getName());
            // this.setCastSkillContext(null);
            this.isActive = false; // Mark skill as inactive
            return false;
        }

        if (currentTick >= nextDamageTick) {
            log.info("MeleeSkill performing damage for champion: {}", this.getSkillOwner().getName());
            this.performAOEDamage();
            this.nextDamageTick += Util.seconds2GameTick(DAMAGE_INTERVAL_SECONDS);
            return true; // Skill is still active, continue to perform
        }

        // Skill is still active, but no damage this tick
        log.debug("MeleeSkill: not in damage tick, current tick: {}, nextDamageTick: {}", 
            currentTick, nextDamageTick);
        return true;
    }

    private void performAOEDamage() {
        CircleShape hitBox = new CircleShape(
            this.getSkillOwner().getCurrentPosition(),
            DAMAGE_RADIUS
        );

        this.getCastSkillContext().addSkillDamage(this.getDamagePerSecond());

        Set<SkillReceiver> hitEntities = this.getSkillOwner().getGameStateService()
            .getSkillReceiverEnemiesInScope(
                this.getSkillOwner().getGameState(), 
                hitBox, this.getSkillOwner().getOwnerSlot());
            
        log.info("MeleeSkill hit {} entities in range for champion: {}", 
            hitEntities.size(), this.getSkillOwner().getName());

        hitEntities.stream()
            .forEach(entity -> {
                this.getCastSkillContext().setTarget(entity);
                entity.receiveSkillDamage(this.getCastSkillContext());
            });
    }
}
