package com.server.game;


import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import com.server.game.map.AStarPathfinder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AStarPathfinderTest {

    @Test
    void testSimplePath() {
        boolean[][] grid = {
            {true, true, true},
            {false, true, false},
            {true, true, true}
        };

        Coordinate start = new Coordinate(0, 0); // hàng 0, cột 0
        Coordinate end = new Coordinate(2, 2);   // hàng 2, cột 2

        List<Coordinate> path = AStarPathfinder.findPath(grid, start, end);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new Coordinate(0, 0), path.get(0));
        assertEquals(new Coordinate(2, 2), path.get(path.size() - 1));
    }

    @Test
    void testNoPathToEnd() {
        boolean[][] grid = {
            {true, false, true},
            {false, false, false},
            {true, true, true}
        };

        Coordinate start = new Coordinate(0, 0);
        Coordinate end = new Coordinate(0, 2); // bị chặn

        List<Coordinate> path = AStarPathfinder.findPath(grid, start, end);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new Coordinate(0, 0), path.get(0));
        // Không đến được end, nên điểm cuối KHÁC end
        assertNotEquals(end, path.get(path.size() - 1));
    }

    @Test
    void testStartEqualsEnd() {
        boolean[][] grid = {
            {true}
        };
        Coordinate point = new Coordinate(0, 0);

        List<Coordinate> path = AStarPathfinder.findPath(grid, point, point);

        assertEquals(1, path.size());
        assertEquals(point, path.get(0));
    }

    @Test
    void testBlockedStart() {
        boolean[][] grid = {
            {false, true},
            {true, true}
        };

        Coordinate start = new Coordinate(0, 0); // blocked
        Coordinate end = new Coordinate(1, 1);

        List<Coordinate> path = AStarPathfinder.findPath(grid, start, end);

        assertTrue(path.isEmpty());
    }

    @Test
    void testBlockedEndButReachableNearby() {
        boolean[][] grid = {
            {true, true, true},
            {true, false, true},
            {true, true, true}
        };

        Coordinate start = new Coordinate(0, 0);
        Coordinate end = new Coordinate(1, 1); // blocked

        List<Coordinate> path = AStarPathfinder.findPath(grid, start, end);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new Coordinate(0, 0), path.get(0));
        // Không bằng end nhưng gần đó
        assertNotEquals(end, path.get(path.size() - 1));
    }

    @Test
    void test7() {
        boolean[][] grid = {
            {true, true,  true,  true,  true},
            {true, true,  true, true, true},
            {true, true,  true,  true, true},
            {true, true, true, true, true},
            {true, true,  true,  true,  true}
        };

        Coordinate start = new Coordinate(0, 0);
        Coordinate end = new Coordinate(4, 4);

        List<Coordinate> path = AStarPathfinder.findPath(grid, start, end);


        System.out.println(">>> [TEST 7] Path:");
        for (Coordinate coord : path) {
            System.out.print("(" + (int)coord.getX() + ", " + (int)coord.getY() + ")  ");
        }
        System.out.println();

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new Coordinate(0, 0), path.get(0));
        assertEquals(new Coordinate(4, 4), path.get(path.size() - 1));
    }

    @Test
    void test8() {
        boolean[][] grid = {
            {true, true,  false,  true,  true},
            {true, true,  false, false, true},
            {false, true,  true,  false, true},
            {true, false, false, false, true},
            {true, false,  true,  true,  true}
        };

        Coordinate start = new Coordinate(0, 0);
        Coordinate end = new Coordinate(4, 4);

        List<Coordinate> path = AStarPathfinder.findPath(grid, start, end);


        System.out.println(">>> [TEST 8] Path:");
        for (Coordinate coord : path) {
            System.out.print("(" + (int)coord.getX() + ", " + (int)coord.getY() + ")  ");
        }
        System.out.println();

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new Coordinate(0, 0), path.get(0));
        assertNotEquals(new Coordinate(4, 4), path.get(path.size() - 1));
    }
}
