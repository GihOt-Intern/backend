package com.server.game.model.game.championSkill;

import java.util.Set;

import com.server.game.model.game.Champion;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.map.shape.CircleShape;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

import com.server.game.model.game.SkillReceivable;
import com.server.game.model.game.component.skillComponent.DurationSkillComponent;


@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class MageSkill extends DurationSkillComponent {

    static final Float HITBOX_LENGTH = 10f;
    static final Float HITBOX_WIDTH = 4f;

    static final Float DURATION_SECONDS = 3f;
    // damage every half second
    static final Float DAMAGE_INTERVAL_SECONDS = .5f;

    static final Float SPEED = 
        HITBOX_LENGTH / (Util.seconds2GameTick(DURATION_SECONDS)); // distance per tick

    MageHitbox hitbox = null;
    

    public MageSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability, DURATION_SECONDS, DAMAGE_INTERVAL_SECONDS);
    }

    @Override
    public boolean canUseWhileAttacking() {
        return false;
    }

    @Override
    public boolean canUseWhileMoving() {
        return false;
    }

    private float getDamagePerIntervalSeconds() {
        // return 40 + 0.2f * this.getSkillOwner().getDefense();

        return 20000f;
    }

    @Override
    protected final void createHitbox() {
        Vector2 center = this.getSkillOwner().getCurrentPosition();
        Vector2 mousePoint = this.getCastSkillContext().getTargetPoint();
        Vector2 direction = center.directionTo(mousePoint);
        this.hitbox = new MageHitbox(center, HITBOX_LENGTH / 2, direction, SPEED);
    }

    @Override // Hitbox moves every tick
    protected final void doUpdatePerTick() {
        log.info("Mage's hitbox is moving...");

        this.hitbox.move();
    }
    

    @Override // Performs damage only at the correct tick
    protected boolean performAtCorrectTick() {

        log.info("Mage performing damage...");

        CircleShape hitBoxShape = this.hitbox.getHitBox();

        this.getCastSkillContext().addSkillDamage(
                this.getDamagePerIntervalSeconds());

        Set<SkillReceivable> hitEntities = this.getSkillOwner().getGameStateService()
            .getSkillReceivableEnemiesInScope(
                this.getSkillOwner().getGameState(),
                hitBoxShape, this.getSkillOwner().getOwnerSlot());
            
        log.info("MageSkill hit {} entities in range at tick for champion: {}", 
            hitEntities.size(), this.getSkillOwner().getName());

        hitEntities.stream()
            .forEach(hitEntity -> {
                this.getCastSkillContext().setTarget(hitEntity);
                hitEntity.receiveSkillDamage(this.getCastSkillContext());
            });

        return true;   
    }


    //**** INNER CLASS ****/
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    public static final class MageHitbox {
        @Delegate
        CircleShape hitBox;
        final Vector2 direction; // has been normalized
        final Float speed; // distance per tick

        public MageHitbox(Vector2 center, Float radius, Vector2 direction, Float speed) {
            this.hitBox = new CircleShape(center, radius);
            this.direction = direction;
            this.speed = speed;
        }

        public void move() {
            Vector2 newCenter = this.hitBox.getCenter()
                .add(this.direction.multiply(this.speed));
            this.hitBox.setCenter(newCenter);
        }
    }
    //**** END INNER CLASS ****/
}



