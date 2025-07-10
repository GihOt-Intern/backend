package com.server.game.map;

import java.util.ArrayList;
import java.util.List;

import com.server.game.map.object.abstraction.Minion;
import com.server.game.map.object.Tower;

import com.server.game.map.object.abstraction.Character;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Team {
    private final Character character;
    private final Tower tower;
    private final List<Minion> minions;


    public Team(Character character, Tower tower) {
        this.character = character;
        this.tower = tower;
        this.minions = new ArrayList<>();
    }

    public void addMinion(Minion minion) {
        this.minions.add(minion);
    }

}
