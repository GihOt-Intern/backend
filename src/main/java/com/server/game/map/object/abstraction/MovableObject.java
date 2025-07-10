package com.server.game.map.object.abstraction;


import com.server.game.map.component.Vector2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public abstract class MovableObject extends MapObject {
    Vector2 velocity;
}
