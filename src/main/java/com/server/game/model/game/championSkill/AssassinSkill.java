package com.server.game.model.game.championSkill;

import java.util.Set;

import com.server.game.model.game.Champion;
import com.server.game.model.game.SkillReceivable;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.map.shape.RectShape;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.ThetaStarPathfinder;

import lombok.extern.slf4j.Slf4j;
// Lướt thẳng tới trước 1 khoảng cách X, gây sát thương lên đối thủ trên đường đi
@Slf4j
public class AssassinSkill extends SkillComponent {

    private static final float DASH_LENGTH = 10.0f;
    private static final float DASH_WIDTH = 5.0f;

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

    private final RectShape getHitbox(Vector2 ownerOldPosition, Vector2 actualDirection) {

        float hitboxLength = DASH_LENGTH + 2f;

        Vector2 centerPoint = ownerOldPosition.add(actualDirection.multiply(hitboxLength / 2));

        return new RectShape(
            centerPoint,
            DASH_WIDTH,
            hitboxLength,
            actualDirection
        );
    }

    private final void dash(Vector2 ownerOldPosition, 
        Vector2 ownerNewExpectedPosition,
        Vector2 ownerActualNewPosition) {

        
        
        // GridCell expectedCell = this.skillOwner
        //     .getGameState().toGridCell(ownerNewExpectedPosition);
        // boolean isExpectedValid = this.skillOwner
        //     .getGameState().getGameMapGrid().isWalkable(expectedCell);
        // log.info("Is expected position {}, cell {} valid: {}", 
        //     ownerNewExpectedPosition, expectedCell, isExpectedValid);


        // GridCell actualCell = this.skillOwner
        //     .getGameState().toGridCell(ownerActualNewPosition);
        // boolean isActualValid = this.skillOwner
        //     .getGameState().getGameMapGrid().isWalkable(actualCell);
        // log.info("Is actual position {}, cell {} valid: {}", 
        //     ownerActualNewPosition, actualCell, isActualValid);
        
    
        // Broadcast cast skill event
        float actualDashLength = ownerOldPosition.distance(ownerActualNewPosition);
        this.castSkillContext.setSkillLength(actualDashLength);

        log.info("Assassin dashing, current position: {}, expect position: {}, actual position: {}, expect dash length: {}, actual dash length: {}",
            ownerOldPosition,
            ownerNewExpectedPosition,
            ownerActualNewPosition,
            DASH_LENGTH,
            actualDashLength
        );

        this.skillOwner.setStopMoving(true); 
        this.skillOwner.setCurrentPosition(ownerActualNewPosition);

        this.getSkillOwner().getGameStateService()
            .sendCastSkillAnimation(this.castSkillContext);
        
    }

    private final void performAOEDamage(RectShape hitbox) {

        this.getCastSkillContext().addSkillDamage(this.getDamage());

        Set<SkillReceivable> hitEntities = this.getSkillOwner().getGameStateService()
            .getSkillReceivableEnemiesInScope(
                this.getSkillOwner().getGameState(),
                hitbox, this.getSkillOwner().getOwnerSlot());

        log.info("{} hit {} entities in range for champion: {}",
            this.getClass().getSimpleName(), hitEntities.size(), this.getSkillOwner().getName());

        hitEntities.stream()
            .forEach(entity -> {
                this.getCastSkillContext().setTarget(entity);
                entity.receiveSkillDamage(this.getCastSkillContext());
            });
    }

    @Override
    protected boolean doUse() {

        // Find dashing end position (check walls colliding,...)
        Vector2 ownerCurrentPosition = this.skillOwner.getCurrentPosition();
        Vector2 mousePoint = this.getCastSkillContext().getTargetPoint();
        Vector2 expectedDirection = ownerCurrentPosition.directionTo(mousePoint);
        Vector2 ownerNewExpectedPosition = ownerCurrentPosition.add(expectedDirection.multiply(DASH_LENGTH));
        Vector2 ownerActualNewPosition = ThetaStarPathfinder.findClosestWalkablePosition(
                this.skillOwner.getGameState(), ownerNewExpectedPosition);

        Vector2 actualDirection = ownerCurrentPosition.directionTo(ownerActualNewPosition);

        // get hit box before dashing
        RectShape hitbox = this.getHitbox(ownerCurrentPosition, actualDirection);
        this.performAOEDamage(hitbox);

        this.dash(ownerCurrentPosition, expectedDirection, ownerActualNewPosition);


        return true;
    }
}
