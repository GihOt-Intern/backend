package com.server.game.model.game;

import java.util.HashMap;
import java.util.Map;

import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.PositionComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.Attackable;
import com.server.game.model.map.component.Vector2;

// abstract class to represent an entity in the game
// (Champion, Troop, Tower, Burg)
public abstract class Entity implements Attackable {


    protected Short ownerSlot;
    protected GameState gameState;


    private final Map<Class<?>, Object> components = new HashMap<>();

    public Entity(Short ownerSlot, GameState gameState){
        this.ownerSlot = ownerSlot;
        this.gameState = gameState;
    }

    public Short getOwnerSlot() { return ownerSlot; }

    public GameState getGameState() { return gameState; }



    // Concrete classes have to implement this method to add all their components
    // to the Map above. This method must be called in the constructor of the concrete class.
    protected abstract void addAllComponents();

    protected <T> void addComponent(Class<T> clazz, T component) {
        components.put(clazz, component);
    }

    public <T> T getComponent(Class<T> clazz) {
        return clazz.cast(components.get(clazz));
    }

    protected boolean hasComponent(Class<?> clazz) {
        return components.containsKey(clazz);
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

    public abstract String getIdAString();
}
