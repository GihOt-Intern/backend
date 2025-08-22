package com.server.game.model.map.component;

import java.util.Objects;

public record GridCell(int r, int c) {

    public GridCell add(int dr, int dc) {
        return new GridCell(r + dr, c + dc);
    }

    @Override
    public String toString() {
        return "(" + r + ", " + c + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        GridCell gridCell = (GridCell) other;
        return r == gridCell.r && c == gridCell.c;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, c);
    }


}
