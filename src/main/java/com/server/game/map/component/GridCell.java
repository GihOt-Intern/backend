package com.server.game.map.component;


public record GridCell(int r, int c) {

    @Override
    public String toString() {
        return "(" + r + ", " + c + ")";
    }


}
