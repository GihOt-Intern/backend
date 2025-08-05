package com.server.game.model.game;

import java.util.HashMap;
import java.util.Map;

import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.PositionComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.Attackable;
import com.server.game.model.game.component.attributeComponent.AttributeComponent;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.move.MoveService;

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
    private PositionComponent positionComponent;


    private final Map<Class<?>, Object> components = new HashMap<>();

    public Entity(String stringId, SlotState ownerSlot, GameState gameState, Vector2 initPosition,
        MoveService moveService) {
        this.stringId = stringId;
        this.ownerSlot = ownerSlot;
        this.gameState = gameState;
        this.positionComponent = new PositionComponent(initPosition, moveService);


        this.addComponent(PositionComponent.class, positionComponent);
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


    public float getSpeed() {
        if (hasComponent(AttributeComponent.class)) {
            return getComponent(AttributeComponent.class).getMoveSpeed();
        }
        // if (hasComponent(ChampionAttributeComponent.class)) { 
        //     return getComponent(ChampionAttributeComponent.class).getMoveSpeed();
        // }
        
        System.out.println("Entity does not have AttributeComponent, returning default speed=0.");
        return 0; // Default value if no attribute component is present
    }

    public void setMove2Target(Entity target) {
        if (hasComponent(PositionComponent.class)) {
            getComponent(PositionComponent.class).getMoveService()
                .setMove(this, target.getCurrentPosition(),false);
        } else {
            throw new UnsupportedOperationException("[setMove2Target] Entity does not have PositionComponent");
        }
    }
    
    public void setStopMoving() {
        if (hasComponent(PositionComponent.class)) {
            getComponent(PositionComponent.class).getMoveService()
                .setMove(this, this.getCurrentPosition(),false);
            // SpringContextHolder.getBean(PositionService.class) // TODO: try
            //     .popPendingPosition(this);
        } else {
            throw new UnsupportedOperationException("[setStopMoving] Entity does not have PositionComponent");
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
        if (hasComponent(PositionComponent.class)) {
            return getComponent(PositionComponent.class).getCurrentPosition();
        }
        System.out.println("Entity does not have PositionComponent, returning null");
        return null;
    }

    public final float distanceTo(Entity other) {
        return this.getComponent(PositionComponent.class)
            .distanceTo(other.getCurrentPosition());
    }

    public void updateNewTargetPosition(Vector2 position) {
        if (hasComponent(PositionComponent.class)) {
            getComponent(PositionComponent.class).setTargetPosition(position);
        } else {
            throw new UnsupportedOperationException("Entity does not have PositionComponent");
        }
    }


    public void updatePosition(Vector2 newPosition) {
        if (hasComponent(PositionComponent.class)) {
            getComponent(PositionComponent.class).setCurrentPosition(newPosition);

            this.afterUpdatePosition(newPosition);
        } else {
            throw new UnsupportedOperationException("Entity does not have PositionComponent");
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
