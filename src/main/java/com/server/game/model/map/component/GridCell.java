package com.server.game.model.map.component;


public record GridCell(int r, int c) {

    @Override
    public String toString() {
        return "(" + r + ", " + c + ")";
    }


}
