package com.server.game.map.object.abstraction;


import com.server.game.map.object.interf4ce.Attackable;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

@AllArgsConstructor
public abstract class Minion extends MovableObject implements Attackable {

    @Delegate
    private HealthComponent healthComponent;

    public Minion(int initialHP) {
        this.healthComponent = new HealthComponent(initialHP);
    }
}
