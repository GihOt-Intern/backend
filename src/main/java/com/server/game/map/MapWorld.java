package com.server.game.map;

import java.util.ArrayList;
import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MapWorld {
    private final int width;
    private final int height;
    private final List<Team> teams;
    private final NavMesh navMesh;

    public MapWorld(int width, int height) {
        this.width = width;
        this.height = height;
        this.navMesh = null;
        this.teams = new ArrayList<>();
    }

    public MapWorld(int width, int height, NavMesh navMesh) {
        this.width = width;
        this.height = height;
        this.navMesh = navMesh;
        this.teams = new ArrayList<>();
    }

    public void addTeam(Team team) {
        this.teams.add(team);
    }
}
