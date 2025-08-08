package com.server.game.model.game.championSkill;

import java.util.Set;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.attackComponent.SkillReceiver;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.map.shape.RectShape;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.ThetaStarPathfinder;

import lombok.extern.slf4j.Slf4j;
// Lướt thẳng tới trước 1 khoảng cách X, gây sát thương lên đối thủ trên đường đi
@Slf4j
public class AssassinSkill extends SkillComponent {

    private static final float DASH_LENGTH = 8.0f;
    private static final float DASH_WIDTH = 2.0f;

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

        
        // Move the champion to the new position after the skill is cast
        // new position is the closest walkable position in the direction of the mouse point
        // to ensure the champion does not get stuck in walls
        // Calculate the new position based on the direction and DASH_LENGTH
        Vector2 ownerCurrentPosition = this.skillOwner.getCurrentPosition();
        Vector2 mousePoint = this.getCastSkillContext().getTargetPoint();
        Vector2 direction = ownerCurrentPosition.directionTo(mousePoint);
        Vector2 ownerNewExpectedPosition = ownerCurrentPosition.add(direction.multiply(DASH_LENGTH));
        Vector2 ownerActualNewPosition = ThetaStarPathfinder.findClosestWalkablePosition(
                this.skillOwner.getGameState(), ownerNewExpectedPosition);

        // Broadcast cast skill event
        float actualDashLength = ownerCurrentPosition.distance(ownerActualNewPosition);
        this.castSkillContext.setSkillLength(actualDashLength);
        this.getSkillOwner().getGameStateService()
            .sendCastSkillAnimation(this.castSkillContext);

        log.info("Assassin dashing, expect position: {}, actual position: {}, expect dash length: {}, actual dash length: {}",
            ownerNewExpectedPosition,
            ownerActualNewPosition,
            DASH_LENGTH,
            actualDashLength
        );

        // Update the champion's position
        log.info("Dash from {} to {} position", 
            ownerCurrentPosition, ownerActualNewPosition);
        this.skillOwner.setStopMoving(true); 
        this.skillOwner.setCurrentPosition(ownerActualNewPosition);

        return true;
    }
}
