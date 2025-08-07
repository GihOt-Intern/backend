package com.server.game.model.game.championSkill;

import java.util.Set;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.attackComponent.SkillReceiver;
import com.server.game.model.game.component.skillComponent.DurationSkillComponent;
import com.server.game.resource.model.ChampionDB.ChampionAbility;

import lombok.extern.slf4j.Slf4j;

import com.server.game.model.map.shape.CircleShape;

// Xoay rìu trong 5s, mỗi giây gây sát thương phạm vi xung quanh 1 ô = 40 + 20% DEF		
@Slf4j
public class MeleeSkill extends DurationSkillComponent {

    private static final float DAMAGE_RADIUS = 5.0f;

    public MeleeSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability, 5.0f, 1.0f);
    }

    @Override
    public boolean canUseWhileAttacking() {
        return false;
    }

    @Override
    public boolean canUseWhileMoving() {
        return true;
    }


    private float getDamagePerSecond() {
        // return 40 + 0.2f * this.getSkillOwner().getDefense();

        return 20000f;
    }

    @Override
    protected boolean performAtCorrectTick() {
        log.info("MeleeSkill performing damage for champion: {}", this.getSkillOwner().getName());

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

        return true;
    }
}
