package com.server.game.util;


import java.util.*;

import com.server.game.model.game.GameState;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMapGrid;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThetaStarPathfinder {

    // 4 directions or 8 directions ???
    private static final int[][] DIRECTIONS = Util.EIGHT_DIRECTIONS;

    public static List<GridCell> findPath(GameMapGrid gameMapGrid, GridCell start, GridCell end) {
        boolean[][] grid = gameMapGrid.getGrid();
        int rows = grid.length;
        int cols = grid[0].length;


        // Nếu điểm bắt đầu hoặc kết thúc không nằm trong lưới -> vô lý -> trả về danh sách rỗng
        // Nhưng điều này sẽ không xảy ra vì khi chuyển từ Vector2 sang GridCell,
        // nó đã được đảm bảo kẹp giữa trong phạm vi của lưới
        if (gameMapGrid.isOutGrid(start) || gameMapGrid.isOutGrid(end)) {
            return Collections.emptyList();
        }

        // Nếu điểm bắt đầu nằm ở ô không đi được
        if (!gameMapGrid.isWalkable(start)) {
            log.info(">>> Start point is not walkable");
            GridCell closestWalkable = findClosestWalkablePosition(grid, start);
            if (closestWalkable == null) {
                log.info(">>> No walkable position found near start point");
                return Collections.emptyList();
            }
            log.info(">>> Using closest walkable position: " + closestWalkable);
            start = closestWalkable;
        }

        // Nếu điểm kết thúc nằm ở ô không đi được
        if (!gameMapGrid.isWalkable(end)) {
            log.info(">>> End point is not walkable");
            GridCell closestWalkable = findClosestWalkablePosition(grid, end);
            if (closestWalkable == null) {
                log.info(">>> No walkable position found near end point");
                return Collections.emptyList();
            }
            log.info(">>> Using closest walkable position: " + closestWalkable);
            end = closestWalkable;
        }

        // Số lượng ô tối đa để tìm kiếm
        final int MAX_NODES_TO_EXPLORE = 2000;
        int nodesExplored = 0;

        // Khoảng cách tối đa
        int distanceLimit = (Math.abs(start.r() - end.r()) + Math.abs(start.c() - end.c())) * 3;
        distanceLimit = Math.min(distanceLimit, rows * cols / 4); // Cap to 1/4 of map size

        // openSet: hàng đợi ưu tiên theo f(n)
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<String, Node> allNodes = new HashMap<>();
        boolean[][] visited = new boolean[rows][cols];

        Node startNode = new Node(start.r(), start.c(), null, 0, 
                                    heuristic(start.r(), start.c(), end.r(), end.c()));
        openSet.add(startNode);
        allNodes.put(key(start.r(), start.c()), startNode);

        Node closest = startNode;
        double closestDistance = heuristic(start.r(), start.c(), end.r(), end.c());

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            nodesExplored++;

            if (current.row == end.r() && current.col == end.c()) {
                log.info(">>> Found path to end point after exploring " + nodesExplored + " nodes");
                return reconstructPath(current);
            }

            visited[current.row][current.col] = true;

            // Optimization
            double currentDistance = heuristic(current.row, current.col, end.r(), end.c());
            if (currentDistance < closestDistance) {
                closest = current;
                closestDistance = currentDistance;
            }

            if (currentDistance > distanceLimit && nodesExplored > MAX_NODES_TO_EXPLORE) {
                log.info(">>> Stopping search due to distance limit or max nodes explored");
                break; // Stop if we exceed distance limit or max nodes explored
            }

            // Cập nhật node gần nhất với end (nếu không tìm được end)
            if (heuristic(current.row, current.col, end.r(), end.c()) <
                heuristic(closest.row, closest.col, end.r(), end.c())) {
                closest = current;
            }

            for (int[] dir : DIRECTIONS) {
                int newRow = current.row + dir[0];
                int newCol = current.col + dir[1];

                if (!isValid(newRow, newCol, grid) || visited[newRow][newCol] || !grid[newRow][newCol]) {
                    continue;
                }

                // Kiểm tra chéo không cắt góc
                if (dir[0] != 0 && dir[1] != 0) {
                    int checkRow1 = current.row + dir[0];
                    int checkCol1 = current.col;
                    int checkRow2 = current.row;
                    int checkCol2 = current.col + dir[1];

                    if (!isValid(checkRow1, checkCol1, grid) || !isValid(checkRow2, checkCol2, grid)
                            || (!grid[checkRow1][checkCol1] && !grid[checkRow2][checkCol2])) {
                        continue;
                    }
                }

                String key = key(newRow, newCol);
                Node neighbor = allNodes.getOrDefault(key, new Node(newRow, newCol));

                double tentativeG;
                Node pathParent;

                if (current.parent != null && lineOfSight(current.parent.row, current.parent.col, newRow, newCol, grid)) {
                    double directCost = Math.hypot(current.parent.row - newRow, current.parent.col - newCol);
                    tentativeG = current.parent.g + directCost;
                    pathParent = current.parent;
                } else {
                    double moveCost = (dir[0] != 0 && dir[1] != 0 ) ? Math.sqrt(2) : 1.0;
                    tentativeG = current.g + moveCost;
                    pathParent = current;
                }

                if (tentativeG < neighbor.g) {
                    neighbor.g = tentativeG;
                    neighbor.h = heuristic(newRow, newCol, end.r(), end.c());
                    neighbor.f = neighbor.g + neighbor.h;
                    neighbor.parent = pathParent;

                    allNodes.put(key, neighbor);
                    openSet.add(neighbor);
                }
            }
        }
        if (nodesExplored >= MAX_NODES_TO_EXPLORE) {
            log.info("Pathfinding stopped after exploring " + nodesExplored + " nodes, returning closest path");
        }

        // Không tìm được đường đi đến end ⇒ trả về đường đi gần nhất
        return reconstructPath(closest);
    }

    public static Vector2 findClosestWalkablePosition(GameState gameState, Vector2 position) {
        GridCell currentCell = gameState.toGridCell(position);
        if (gameState.getGameMapGrid().isWalkable(currentCell)) {
            return position; // Nếu ô hiện tại có thể đi được
        }
        GridCell closestWalkable = findClosestWalkablePosition(
            gameState.getGameMapGrid().getGrid(), currentCell);
        
        if (closestWalkable != null) {
            return gameState.toPosition(closestWalkable);
        } else {
            log.info(">>> No walkable position found near " + position);
            return null; // Không tìm thấy ô đi được gần nhất
        }
    }

    private static GridCell findClosestWalkablePosition(boolean[][] grid, GridCell target) {
        int rows = grid.length;
        int cols = grid[0].length;
        
        // Use A* to find closest walkable position
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<String, Node> allNodes = new HashMap<>();
        boolean[][] visited = new boolean[rows][cols];
        
        // Start from target position
        Node startNode = new Node(target.r(), target.c(), null, 0, 0);
        startNode.f = 0; // Initial f-value is 0
        openSet.add(startNode);
        allNodes.put(key(target.r(), target.c()), startNode);
        
        int maxNodes = 1000; // Limit search to prevent excessive exploration
        int nodesExplored = 0;
        
        while (!openSet.isEmpty() && nodesExplored < maxNodes) {
            Node current = openSet.poll();
            nodesExplored++;
            
            // If current position is walkable, return it immediately
            if (grid[current.row][current.col]) {
                log.info(">>> Found walkable position after exploring " + 
                    nodesExplored + " nodes: (" + current.row + "," + current.col + ")");
                return new GridCell(current.row, current.col);
            }
            
            visited[current.row][current.col] = true;
            
            // Check all 8 directions
            for (int[] dir : DIRECTIONS) {
                int newRow = current.row + dir[0];
                int newCol = current.col + dir[1];
                
                if (!isValid(newRow, newCol, grid) || visited[newRow][newCol]) {
                    continue;
                }
                
                String key = key(newRow, newCol);
                Node neighbor = allNodes.getOrDefault(key, new Node(newRow, newCol));
                
                // Calculate movement cost (diagonal is sqrt(2), cardinal is 1)
                double moveCost = (dir[0] != 0 && dir[1] != 0) ? Math.sqrt(2) : 1.0;
                double tentativeG = current.g + moveCost;
                
                if (tentativeG < neighbor.g) {
                    neighbor.g = tentativeG;
                    
                    // For finding closest walkable position:
                    // - g = distance from target
                    // - h = 0 (we're not trying to reach a specific goal)
                    // - f = g (prioritize cells closer to target)
                    neighbor.h = 0;
                    neighbor.f = neighbor.g;
                    neighbor.parent = current;
                    
                    allNodes.put(key, neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        log.info(">>> No walkable position found after exploring " + nodesExplored + " nodes");
        return null; // No walkable position found
    }

    private static double heuristic(int r1, int c1, int r2, int c2) {
        // Dùng khoảng cách Euclidean cho di chuyển chéo
        return Math.hypot(r1 - r2, c1 - c2);
    }

    private static boolean lineOfSight(int x0, int y0, int x1, int y1, boolean[][] grid) {
        int dx = Math.abs(x1-x0);
        int dy = Math.abs(y1-y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (x != x1 || y != y1) {
            if (!isValid(x, y, grid) || !grid[x][y]) {
                return false; // Không có đường đi
            }
            int err2 = err * 2;
            if (err2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (err2 < dx) {
                err += dx;
                y += sy;
            }
        }
        return true; // Đã đến đích
    }

    private static boolean isValid(int row, int col, boolean[][] grid) {
        return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length;
    }

    private static String key(int row, int col) {
        return row + "," + col;
    }

    private static List<GridCell> reconstructPath(Node node) {
        // Use LinkedList to efficiently add elements at the beginning
        LinkedList<GridCell> path = new LinkedList<>();
        while (node != null) {
            path.addFirst(new GridCell(node.row, node.col));
            node = node.parent;
        }

        // print path for debugging
        log.info("Path found: " + path);

        // Convert LinkedList to ArrayList to access by index later
        return new ArrayList<>(path);
    }


    
    // Inner class đại diện cho mỗi node
    private static class Node {
        int row, col;
        double g = Double.MAX_VALUE;
        double h = 0;
        double f = Double.MAX_VALUE;
        Node parent;

        Node(int row, int col) {
            this.row = row;
            this.col = col;
        }

        Node(int row, int col, Node parent, double g, double h) {
            this.row = row;
            this.col = col;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }
    }
}
