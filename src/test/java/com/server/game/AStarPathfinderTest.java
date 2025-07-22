package com.server.game;


import org.junit.jupiter.api.Test;
import com.server.game.map.component.GridCell;

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

        GridCell start = new GridCell(0, 0); // hàng 0, cột 0
        GridCell end = new GridCell(2, 2);   // hàng 2, cột 2

        List<GridCell> path = AStarPathfinder.findPath(grid, start, end);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new GridCell(0, 0), path.get(0));
        assertEquals(new GridCell(2, 2), path.get(path.size() - 1));
    }

    @Test
    void testNoPathToEnd() {
        boolean[][] grid = {
            {true, false, true},
            {false, false, false},
            {true, true, true}
        };

        GridCell start = new GridCell(0, 0);
        GridCell end = new GridCell(0, 2); // bị chặn

        List<GridCell> path = AStarPathfinder.findPath(grid, start, end);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new GridCell(0, 0), path.get(0));
        // Không đến được end, nên điểm cuối KHÁC end
        assertNotEquals(end, path.get(path.size() - 1));
    }

    @Test
    void testStartEqualsEnd() {
        boolean[][] grid = {
            {true}
        };
        GridCell point = new GridCell(0, 0);

        List<GridCell> path = AStarPathfinder.findPath(grid, point, point);

        assertEquals(1, path.size());
        assertEquals(point, path.get(0));
    }

    @Test
    void testBlockedStart() {
        boolean[][] grid = {
            {false, true},
            {true, true}
        };

        GridCell start = new GridCell(0, 0); // blocked
        GridCell end = new GridCell(1, 1);

        List<GridCell> path = AStarPathfinder.findPath(grid, start, end);

        assertTrue(path.isEmpty());
    }

    @Test
    void testBlockedEndButReachableNearby() {
        boolean[][] grid = {
            {true, true, true},
            {true, false, true},
            {true, true, true}
        };

        GridCell start = new GridCell(0, 0);
        GridCell end = new GridCell(1, 1); // blocked

        List<GridCell> path = AStarPathfinder.findPath(grid, start, end);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new GridCell(0, 0), path.get(0));
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

        GridCell start = new GridCell(0, 0);
        GridCell end = new GridCell(4, 4);

        List<GridCell> path = AStarPathfinder.findPath(grid, start, end);


        System.out.println(">>> [TEST 7] Path:");
        for (GridCell cell : path) {
            System.out.print(cell + "  ");
        }
        System.out.println();

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new GridCell(0, 0), path.get(0));
        assertEquals(new GridCell(4, 4), path.get(path.size() - 1));
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

        GridCell start = new GridCell(0, 0);
        GridCell end = new GridCell(4, 4);

        List<GridCell> path = AStarPathfinder.findPath(grid, start, end);


        System.out.println(">>> [TEST 8] Path:");
        for (GridCell cell : path) {
            System.out.print(cell + "  ");
        }
        System.out.println();

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new GridCell(0, 0), path.get(0));
        assertNotEquals(new GridCell(4, 4), path.get(path.size() - 1));
    }

    @Test
    void test9() {
        boolean[][] grid = {
            {true, true,  false,  true,  true},
            {true, true,  false, false, true},
            {false, true,  true,  false, true},
            {true, false, false, false, true},
            {true, false,  true,  true,  false}
        };

        GridCell start = new GridCell(0, 0);
        GridCell end = new GridCell(4, 4);

        List<GridCell> path = AStarPathfinder.findPath(grid, start, end);


        System.out.println(">>> [TEST 9] Path:");
        for (GridCell cell : path) {
            System.out.print(cell + "  ");
        }
        System.out.println();

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(new GridCell(0, 0), path.get(0));
        assertNotEquals(new GridCell(4, 4), path.get(path.size() - 1));
    }
}
