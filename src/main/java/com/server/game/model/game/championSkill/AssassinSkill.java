package com.server.game.model.game.championSkill;

import java.util.Set;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.attackComponent.SkillReceiver;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.map.shape.RectShape;
import com.server.game.resource.model.ChampionDB.ChampionAbility;

import lombok.extern.slf4j.Slf4j;
// Lướt thẳng tới trước 1 khoảng cách X, gây sát thương lên đối thủ trên đường đi
@Slf4j
public class AssassinSkill extends SkillComponent {

    private static final float DASH_LENGTH = 8.0f;
    private static final float DASH_WIDTH = 8.0f;

    public AssassinSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability);
    }

    private float getDamage() {
        return 20000f;
    }


    @Override
    public boolean canUseWhileAttacking() {
        return false;
    }

    @Override
    public boolean canUseWhileMoving() {
        return false;
    }

    private final RectShape getHitbox() {
        Vector2 ownerPoint = this.skillOwner.getCurrentPosition();
        Vector2 mousePoint = this.getCastSkillContext().getTargetPoint();
        Vector2 direction = ownerPoint.directionTo(mousePoint);

        Vector2 centerPoint = ownerPoint.add(direction.multiply(DASH_LENGTH / 2));
        
        return new RectShape(
            centerPoint,
            DASH_WIDTH,
            DASH_LENGTH,
            direction
        );
    }

    @Override
    protected boolean doUse() {
        RectShape hitbox = this.getHitbox();

        this.getCastSkillContext().addSkillDamage(this.getDamage());

        Set<SkillReceiver> hitEntities = this.getSkillOwner().getGameStateService()
            .getSkillReceiverEnemiesInScope(
                this.getSkillOwner().getGameState(),
                hitbox, this.getSkillOwner().getOwnerSlot());

        log.info("MeleeSkill hit {} entities in range for champion: {}",
            hitEntities.size(), this.getSkillOwner().getName());

        hitEntities.stream()
            .forEach(entity -> {
                this.getCastSkillContext().setTarget(entity);
                entity.receiveSkillDamage(this.getCastSkillContext());
            });

        
        // Broadcast cast skill event
        this.castSkillContext.setSkillLength(DASH_LENGTH);

        this.getSkillOwner().getGameStateService()
            .sendCastSkillAnimation(this.castSkillContext);

        // Move the champion to the end of the dash
        Vector2 ownerCurrentPosition = this.skillOwner.getCurrentPosition();
        Vector2 mousePoint = this.getCastSkillContext().getTargetPoint();
        Vector2 direction = ownerCurrentPosition.directionTo(mousePoint);
        Vector2 ownerNewPosition = ownerCurrentPosition.add(direction.multiply(DASH_LENGTH));
        this.skillOwner.setCurrentPosition(ownerNewPosition);

        return true;
    }
}
