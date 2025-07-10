package com.server.game.map.object.abstraction;


import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public abstract class StaticMapObject extends MapObject {
    // StaticMapObject is a base class for all static objects in the game world
    // It does not have velocity or movement logic, but can be used for collision detection and positioning

    // StaticMapObjects can be used for obstacles, scenery, etc.
    // They are typically not updated every tick like MovableMapObjects

    // Additional properties or methods specific to static objects can be added here
}
