package com.server.game.map.object.abstraction;

import com.server.game.map.component.Vector2;
import com.server.game.map.shape.CollisionShape;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class MapObject {
    protected Vector2 position;
    protected CollisionShape shape; // height and width if rectangle, radius if circle
}
