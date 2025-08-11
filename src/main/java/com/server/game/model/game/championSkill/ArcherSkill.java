package com.server.game.model.game.championSkill;

import java.util.Set;

import com.server.game.model.game.Champion;
import com.server.game.model.game.SkillReceiverEntity;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.map.shape.RectShape;
import com.server.game.resource.model.ChampionDB.ChampionAbility;

import lombok.extern.slf4j.Slf4j;

// Bắn mũi tên định hướng, gây sát thương lên tất cả đối thủ trên đường đi					

@Slf4j
public class ArcherSkill extends SkillComponent {

    private static final float ARCHER_LENGTH = 8.0f;
    private static final float ARCHER_WIDTH = 4.0f;

    public ArcherSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability);
    }

    private float getDamage() {
        return 20000f;
    }

    @Override
    public boolean canUseWhileAttacking() {
        return true;
    }

    @Override
    public boolean canUseWhileMoving() {
        return false;
    }

    private final RectShape getHitbox() {
        Vector2 ownerPoint = this.skillOwner.getCurrentPosition();
        Vector2 mousePoint = this.getCastSkillContext().getTargetPoint();
        Vector2 direction = ownerPoint.directionTo(mousePoint);

        Vector2 centerPoint = ownerPoint.add(direction.multiply(ARCHER_LENGTH / 2));

        return new RectShape(
            centerPoint,
            ARCHER_WIDTH,
            ARCHER_LENGTH,
            direction
        );
    }

    @Override
    protected boolean doUse() {
        RectShape hitbox = this.getHitbox();

        this.getCastSkillContext().addSkillDamage(this.getDamage());

        Set<SkillReceiverEntity> hitEntities = this.getSkillOwner().getGameStateService()
            .getSkillReceiverEnemiesInScope(
                this.getSkillOwner().getGameState(),
                hitbox, this.getSkillOwner().getOwnerSlot());

        log.info("{} hit {} entities in range for champion: {}",
            this.getClass().getSimpleName(), hitEntities.size(),
            this.getSkillOwner().getName());

        hitEntities.stream()
            .forEach(entity -> {
                this.getCastSkillContext().setTarget(entity);
                entity.receiveSkillDamage(this.getCastSkillContext());
            });

        return true;
    }
}
