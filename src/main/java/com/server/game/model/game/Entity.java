package com.server.game.model.game;

import java.util.HashMap;
import java.util.Map;

import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.MovingComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.Attackable;
import com.server.game.model.game.component.attributeComponent.AttributeComponent;
import com.server.game.model.game.component.attributeComponent.ChampionAttributeComponent;
import com.server.game.model.game.component.skillComponent.DurationSkillComponent;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;

import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

// abstract class to represent an entity in the game
// (Champion, Troop, Tower, Burg)
@Getter
@Slf4j
public abstract class Entity implements Attackable {

    protected final String stringId;
    @Delegate
    protected final GameState gameState;

    private final Map<Class<?>, Object> components = new HashMap<>();

    public Entity(String stringId, GameState gameState) {
        this.stringId = stringId;
        this.gameState = gameState;
    }

    // Concrete classes have to implement this method to add all their components
    // to the Map above. This method must be called in the constructor of the concrete classes.
    protected abstract void addAllComponents();

    // Add a component to the entity, also adds it to all its superclasses and interfaces
    protected final <T> void addComponent(Class<T> clazz, T component) {
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
        components.put(current, component);
        for (Class<?> iface : current.getInterfaces()) {
            components.put(iface, component);
        }
        current = current.getSuperclass();
    }
}

    public final <T> T getComponent(Class<T> clazz) {
        return clazz.cast(components.get(clazz));
    }

    protected final boolean hasComponent(Class<?> clazz) {
        return components.containsKey(clazz);
    }

    protected abstract void handleDeath(Entity killer);

    public SlotState getOwnerSlot() {
        if (this instanceof DependentEntity dependentEntity) {
            return dependentEntity.getOwnerSlot();
        }
        // log.info("Entity does not have an owner slot, returning null.");
        return null;
    }

    public boolean isAllies(Entity other) {
        if (this.getOwnerSlot() != null && other.getOwnerSlot() != null) {
            return this.getOwnerSlot().getSlot() == other.getOwnerSlot().getSlot();
        }
        log.info("One of the entities does not have an owner slot, returning false for isAllies.");
        return false; // Default value if no owner slot is present
    }

    public boolean setMoveContext(MoveContext ctx, boolean isForced) {
        if (hasComponent(MovingComponent.class)) {
            System.out.println(">>> [Log in Entity.setMoveContext] Move context set: " + ctx);
            return getComponent(MovingComponent.class).setMoveContext(ctx, isForced);
        } else {
            throw new UnsupportedOperationException("Entity does not have MovingComponent");
        }
    }

    public float getMoveSpeed() {
        if (hasComponent(MovingComponent.class)) {
            return getComponent(MovingComponent.class).getOwnerSpeed();
        }

        System.out.println("Entity does not have MovingComponent, returning default speed=0.");
        return 0; // Default value if no attribute component is present
    }

    public boolean setStopMoving(boolean isForced) {
        if (hasComponent(MovingComponent.class)) {
            return getComponent(MovingComponent.class).setMoveContext(null, isForced);
        } else {
            throw new UnsupportedOperationException("Entity does not have MovingComponent");
        }
    }

    public boolean performMoveAndBroadcast() {
        if (hasComponent(MovingComponent.class)) {
            return getComponent(MovingComponent.class).performMoveAndBroadcast();
        } else {
            log.debug("Entity {} does not have MovingComponent, cannot perform move.", this.stringId);
            return false;
        }
    }
    
    public int getGoldMineDamage() {
        if (hasComponent(ChampionAttributeComponent.class)) {
            return getComponent(ChampionAttributeComponent.class).getGoldMineDamage();
        }
        System.out.println("Entity does not have AttackComponent, returning default gold mine damage=0.");
        return 0; // Default value if no attack component is present
    }

    public void increaseGold(int amount) {
        if (this instanceof DependentEntity dependentEntity) {
            dependentEntity.increaseGold(amount);
            log.info("Entity {} increased gold by {}, new total: {}", this.stringId, amount, dependentEntity.getCurrentGold());
            return;
        }
        log.warn("Entity {} is not a DependentEntity, cannot increase gold.", this.stringId);
    }
    
    public void decreaseGold(int amount) {
        if (this instanceof DependentEntity dependentEntity) {
            dependentEntity.decreaseGold(amount);
            log.info("Entity {} decreased gold by {}, new total: {}", this.stringId, amount, dependentEntity.getCurrentGold());
            return;
        }
        log.warn("Entity {} is not a DependentEntity, cannot decrease gold.", this.stringId);
    }

    public int peekGold() {
        if (this instanceof DependentEntity dependentEntity) {
            return dependentEntity.getCurrentGold();
        }
        log.warn("Entity {} is not a DependentEntity, cannot peek gold.", this.stringId);
        return 0; // Default value if not a DependentEntity
    }

    public float getAttackRange() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).getAttackRange();
        }
        System.out.println("Entity does not have AttackComponent, returning default attack range=0.");
        return 0; // Default value if no attack component is present
    }

    public Entity getAttackTarget() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).getAttackTarget();
        }
        System.out.println("Entity does not have AttackComponent, returning null for getAttackTarget.");
        return null; // Default value if no attack component is present
    }

    public boolean isAttacking() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).isAttacking();
        }
        System.out.println("Entity does not have AttackComponent, returning false for isAttacking.");
        return false; // Default value if no attack component is present
    }
    
    public void stopAttacking() {
        if (hasComponent(AttackComponent.class)) {
            getComponent(AttackComponent.class).stopAttacking();
        } else {
            System.out.println("Entity does not have AttackComponent, cannot stop attacking.");
        }
    }

    public boolean inAttackRange() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).inAttackRange();
        }
        System.out.println("Entity does not have AttackComponent, returning false for isInAttackRange.");
        return false; // Default value if no attack component is present
    }

    public float getDistanceNeededToReachAttackRange() {
        if (hasComponent(AttackComponent.class)) {
            AttackComponent attackComponent = getComponent(AttackComponent.class);
            if (attackComponent.getAttackContext() != null && 
                attackComponent.getAttackContext().getTarget() != null) {
                Vector2 targetPosition = attackComponent.getAttackContext().getTarget().getCurrentPosition();
                return this.getCurrentPosition().distance(targetPosition) - attackComponent.getAttackRange();
            }
        }
        System.out.println("Entity does not have AttackComponent or target, returning default distance=999999f.");
        return 999999f; // Default value if no attack component or target is present
    }

    public float getAttackSpeed() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).getAttackSpeed();
        }
        System.out.println("Entity does not have AttackComponent, returning default attack speed=0.");
        return 0; // Default value if no attack component is present
    }

    public int getDamage() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).getDamage();
        }
        System.out.println("Entity does not have AttackComponent, returning default damage=0.");
        return 0; // Default value if no attack component is present
    }

    public Integer getDefense() {
        if (hasComponent(AttributeComponent.class)) {
            return getComponent(AttributeComponent.class).getDefense();
        }
        System.out.println("Entity does not have AttributeComponent, returning default defense=0.");
        return 0; // Default value if no attribute component is present
    }

    public int getCurrentHP() {
        if (hasComponent(HealthComponent.class)) {
            return getComponent(HealthComponent.class).getCurrentHP();
        }
        System.out.println("Entity does not have HealthComponent, returning default current HP=0.");
        return 0; // Default value if no health component is present
    }

    public int getMaxHP() {
        if (hasComponent(HealthComponent.class)) {
            return getComponent(HealthComponent.class).getMaxHP();
        }
        System.out.println("Entity does not have HealthComponent, returning default max HP=0.");
        return 0; // Default value if no health component is present
    }


    public Vector2 getCurrentPosition() {
        if (hasComponent(MovingComponent.class)) {
            return getComponent(MovingComponent.class).getCurrentPosition();
        }
        if (this instanceof HasFixedPosition fixedPositionEntity) {
            return fixedPositionEntity.getPosition();
        }
        System.out.println("Entity does not have MovingComponent or not a FixedPositionEntity, returning null");
        return null;
    }

    public GridCell getCurrentGridCell() {
        if (hasComponent(MovingComponent.class)) {
            return gameState.toGridCell(this.getCurrentPosition());
        }
        if (this instanceof HasFixedPosition fixedPositionEntity) {
            return gameState.toGridCell(fixedPositionEntity.getPosition());
        }
        System.out.println("Entity does not have MovingComponent or not a FixedPositionEntity, returning null");
        return null;
    }

    public final float distanceTo(Entity other) {

        Vector2 myPosition = this.getCurrentPosition();
        // For non-building entities (troops, champions), their dimensions are treated as zero
        Float myWidth = 0f;
        Float myLength = 0f;

        if (this instanceof Building myBuilding) {
            myWidth = myBuilding.getWidth();
            myLength = myBuilding.getLength();
        }

        Vector2 otherPosition = other.getCurrentPosition();
        // For non-building entities (troops, champions), their dimensions are treated as zero
        Float otherWidth = 0f;
        Float otherLength = 0f;

        if (other instanceof Building otherBuilding) {
            otherWidth = otherBuilding.getWidth();
            otherLength = otherBuilding.getLength();
        }

        float dx = Math.max(0, 
            Math.abs(myPosition.x() - otherPosition.x()) - 
                (myWidth + otherWidth) / 2);
        float dy = Math.max(0, 
            Math.abs(myPosition.y() - otherPosition.y()) - 
                (myLength + otherLength) / 2);

        return (float) Math.sqrt(dx * dx + dy * dy);
    }


    public void updatePosition(Vector2 newPosition) {
        if (hasComponent(MovingComponent.class)) {
            getComponent(MovingComponent.class).setCurrentPosition(newPosition);
        } else {
            throw new UnsupportedOperationException("Entity does not have MovingComponent");
        }
    }

    /**
     * MUST be overridden by subclasses which have MovingComponent.
     * If subclass has nothing to do after updating position,
     * it can just call super.beforeUpdatePosition().
     */
    public void beforeUpdatePosition() {
        // log.info("beforeUpdatePosition in Entity called for updating grid cell...");

        this.getGameStateService()
            .removeEntityFromGridCellMapping(this);
    }

    /**
     * MUST be overridden by subclasses which have MovingComponent.
     * If subclass has nothing to do after updating position,
     * it can just call super.afterUpdatePosition().
     */
    public void afterUpdatePosition() {

        // log.info("afterUpdatePosition in Entity called for updating grid cell...");

        this.getGameStateService()
            .addEntityToGridCellMapping(this);

    }

    public boolean performAttack() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).performAttack();
        } else {
            log.debug("Entity {} does not have AttackComponent, cannot perform attack.", this.stringId);
            return false; 
        }
    }

    public boolean isAlive() {
        if (hasComponent(HealthComponent.class)) {
            return getComponent(HealthComponent.class).isAlive();
        }
        System.out.println("Entity does not have HealthComponent, returning true as default.");
        return true; // Default value if no health component is present
    }

    public boolean isCastingDurationSkill() {
        if (hasComponent(DurationSkillComponent.class)) {
            return getComponent(DurationSkillComponent.class).isActive();
        }
        return false; // Default value if no skill component is present
    }

    public boolean canUseSkillWhileAttacking() {
        if (hasComponent(SkillComponent.class)) {
            return getComponent(SkillComponent.class).canUseWhileAttacking();
        }
        System.out.println("Entity does not have SkillComponent, returning true for canUseSkillWhileAttacking.");
        return true; // Default value if no skill component is present
    }

    public boolean canUseSkillWhileMoving() {
        if (hasComponent(SkillComponent.class)) {
            return getComponent(SkillComponent.class).canUseWhileMoving();
        }
        System.out.println("Entity does not have SkillComponent, returning true for canUseSkillWhileMoving.");
        return true; // Default value if no skill component is present
    }


    public boolean setAttackContext(AttackContext ctx) {
        if (hasComponent(AttackComponent.class)) {
            log.info(">>> [Log in Entity.setAttackContext] Attack context set: {}", ctx);
            return getComponent(AttackComponent.class).setAttackContext(ctx);
        } else {
            log.info("Entity does not have AttackComponent, cannot set attack context.");
            return false; // Default value if no attack component is present
        }
    }

    public AttackContext getAttackContext() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).getAttackContext();
        } else {
            throw new UnsupportedOperationException("Entity does not have AttackComponent");
        }
    }
}
