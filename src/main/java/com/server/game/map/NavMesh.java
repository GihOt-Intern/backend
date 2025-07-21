// package com.server.game.map;

// import java.util.List;

// import com.server.game.map.component.NavPolygon;
// import com.server.game.map.component.Vector2;

// import lombok.AllArgsConstructor;

// @AllArgsConstructor
// public class NavMesh {
//     private final List<NavPolygon> polygons;

//     // Return true if the point is inside any polygon in mesh 
//     public boolean contains(Vector2 point) {
//         return polygons.stream().anyMatch(p -> p.contains(point));
//     }

//     // Return the polygon that contains the point, or null if not found
//     public NavPolygon getPolygon(Vector2 point) {
//         for (NavPolygon polygon : polygons) {
//             if (polygon.contains(point)) {
//                 return polygon;
//             }
//         }
//         return null;
//     }

    
//     public List<Vector2> findPath(Vector2 start, Vector2 end) {
//         // A* giữa các polygon (như đã nói trước đó)
        
        
//         return null;
//     }
// }
