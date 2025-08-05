package com.server.game.model.game;

import java.util.HashMap;
import java.util.Map;

import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.MovingComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.Attackable;
import com.server.game.model.game.component.attributeComponent.AttributeComponent;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.Vector2;

import lombok.experimental.Delegate;
import lombok.Getter;

// abstract class to represent an entity in the game
// (Champion, Troop, Tower, Burg)
@Getter
public abstract class Entity implements Attackable {

    protected String stringId;
    protected SlotState ownerSlot;
    @Delegate
    protected GameState gameState;

    @Delegate
    private MovingComponent movingComponent;

    private final Map<Class<?>, Object> components = new HashMap<>();

    public Entity(String stringId, SlotState ownerSlot, GameState gameState,
        Vector2 initPosition, float ownerSpeed) {
        this.stringId = stringId;
        this.ownerSlot = ownerSlot;
        this.gameState = gameState;
        this.movingComponent = new MovingComponent(this, initPosition, ownerSpeed);


        this.addComponent(MovingComponent.class, movingComponent);
    }

    // Concrete classes have to implement this method to add all their components
    // to the Map above. This method must be called in the constructor of the concrete class.
    protected abstract void addAllComponents();

    // protected <T> void addComponent(Class<T> clazz, T component) {
    //     components.put(clazz, component);
    // }

    // Add a component to the entity, also adds it to all its superclasses and interfaces
    protected <T> void addComponent(Class<T> clazz, T component) {
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
        components.put(current, component);
        for (Class<?> iface : current.getInterfaces()) {
            components.put(iface, component);
        }
        current = current.getSuperclass();
    }
}

    public <T> T getComponent(Class<T> clazz) {
        return clazz.cast(components.get(clazz));
    }

    protected boolean hasComponent(Class<?> clazz) {
        return components.containsKey(clazz);
    }

    public void setMoveContext(MoveContext ctx) {
        if (hasComponent(MovingComponent.class)) {
            getComponent(MovingComponent.class).setMoveContext(ctx);
            System.out.println(">>> [Log in Entity.setMoveContext] Move context set: " + ctx);
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
    
    public void setMoveTargetPoint(Vector2 targetPoint) {
        if (hasComponent(MovingComponent.class)) {
            getComponent(MovingComponent.class).setMoveTargetPoint(targetPoint);
            System.out.println(">>> [Log in Entity.setMoveTargetPoint] Move target point set: " + targetPoint);
        } else {
            throw new UnsupportedOperationException("Entity does not have MovingComponent");
        }
    }

    public void setStopMoving() {
        if (hasComponent(MovingComponent.class)) {
            getComponent(MovingComponent.class).setMoveContext(null);
            System.out.println(">>> [Log in Entity.setStopMoving] Stopped moving.");
        } else {
            throw new UnsupportedOperationException("Entity does not have MovingComponent");
        }
    }

    public void performMoveAndBroadcast() {
        if (hasComponent(MovingComponent.class)) {
            getComponent(MovingComponent.class).performMoveAndBroadcast();
        } else {
            throw new UnsupportedOperationException("Entity does not have MovingComponent");
        }
    }
    

    public float getAttackRange() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).getAttackRange();
        }
        System.out.println("Entity does not have AttackComponent, returning default attack range=0.");
        return 0; // Default value if no attack component is present
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

    public int getDefense() {
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
        System.out.println("Entity does not have MovingComponent, returning null");
        return null;
    }

    public final float distanceTo(Entity other) {
        return this.getComponent(MovingComponent.class)
            .distanceTo(other.getCurrentPosition());
    }


    public void updatePosition(Vector2 newPosition) {
        if (hasComponent(MovingComponent.class)) {
            getComponent(MovingComponent.class).setCurrentPosition(newPosition);

            this.afterUpdatePosition(newPosition);
        } else {
            throw new UnsupportedOperationException("Entity does not have MovingComponent");
        }
    }

    protected void afterUpdatePosition(Vector2 newPosition) {
        this.getGameStateService()
            .updateEntityGridCellMapping(this.gameState, this);
    }

    public boolean performAttack() {
        if (hasComponent(AttackComponent.class)) {
            return getComponent(AttackComponent.class).performAttack();
        } else {
            throw new UnsupportedOperationException("Entity does not have AttackComponent");
        }
    }

    public boolean isAlive() {
        if (hasComponent(HealthComponent.class)) {
            return getComponent(HealthComponent.class).isAlive();
        }
        System.out.println("Entity does not have HealthComponent, returning true as default.");
        return true; // Default value if no health component is present
    }

    public void setAttackContext(AttackContext ctx) {
        if (hasComponent(AttackComponent.class)) {
            getComponent(AttackComponent.class).setAttackContext(ctx);
            System.out.println(">>> [Log in Entity.setAttackContext] Attack context set: " + ctx);
        } else {
            throw new UnsupportedOperationException("Entity does not have AttackComponent");
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
