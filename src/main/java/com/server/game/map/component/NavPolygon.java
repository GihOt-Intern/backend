package com.server.game.map.component;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NavPolygon {
    private final int id;
    private List<Vector2> vertices;
    private List<NavPolygon> neighbors;
    private boolean isWalkable;

    // ray crossing algorithm
    public boolean contains(Vector2 point) {
        return point.inside(this);
    }    

    public int getNumVertices() {
        return vertices.size();
    }

    public Vector2 getVertex(int index) {
        if (index < 0 || index >= vertices.size()) {
            throw new IndexOutOfBoundsException("Index out of bounds for polygon vertices");
        }
        return vertices.get(index);
    }

    // Check if this polygon is adjacent to another polygon
    // Two polygons are adjacent if they have at least one FULLY shared edge
    boolean adjacent(NavPolygon other) {

        int thisNumVertices = this.vertices.size();
        int otherNumVertices = other.vertices.size();


        for (int i = 0; i < thisNumVertices; i++) {
            Vector2 a1 = this.getVertex(i);
            Vector2 a2 = this.getVertex((i + 1) % thisNumVertices);

            for (int j = 0; j < otherNumVertices; j++) {
                Vector2 b1 = other.getVertex(j);
                Vector2 b2 = other.getVertex((j + 1) % otherNumVertices);

                if ((a1.equals(b2) && a2.equals(b1)) || (a1.equals(b1) && a2.equals(b2))) {
                    return true; 
                }
            }
        }
        return false;
    }

    
}
