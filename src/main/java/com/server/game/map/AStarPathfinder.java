package com.server.game.map;

import org.locationtech.jts.geom.Coordinate;

import java.util.*;

public class AStarPathfinder {

    // 4 directions or 8 directions ???
    private static final int[][] DIRECTIONS = {
        {-1, 0},  // up
        {1, 0},   // down
        {0, -1},  // left
        {0, 1},   // right
        {-1, -1}, // diagonal up left
        {-1, 1},  // diagonal up right
        {1, -1},  // diagonal down left
        {1, 1}    // diagonal down right
    };

    public static List<Coordinate> findPath(boolean[][] grid, Coordinate start, Coordinate end) {
        int rows = grid.length;
        int cols = grid[0].length;

        // X = row, Y = col
        int startRow = (int) start.getX();
        int startCol = (int) start.getY();
        int endRow = (int) end.getX();
        int endCol = (int) end.getY();

        // Nếu điểm bắt đầu hoặc kết thúc không đi được
        if (!isValid(startRow, startCol, grid) || !grid[startRow][startCol]) {
            return Collections.emptyList();
        }

        // openSet: hàng đợi ưu tiên theo f(n)
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<String, Node> allNodes = new HashMap<>();
        boolean[][] visited = new boolean[rows][cols];

        Node startNode = new Node(startRow, startCol, null, 0, heuristic(startRow, startCol, endRow, endCol));
        openSet.add(startNode);
        allNodes.put(key(startRow, startCol), startNode);

        Node closest = startNode;

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.row == endRow && current.col == endCol) {
                return reconstructPath(current);
            }

            visited[current.row][current.col] = true;

            // Cập nhật node gần nhất với end (nếu không tìm được end)
            if (heuristic(current.row, current.col, endRow, endCol) <
                heuristic(closest.row, closest.col, endRow, endCol)) {
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

                // Nếu đi thẳng thì cost = 1, nếu đi chéo thì cost = sqrt(2)
                double cost = (dir[0] != 0 && dir[1] != 0) ? Math.sqrt(2) : 1.0;
                double tentativeG = current.g + cost;
                String key = key(newRow, newCol);
                Node neighbor = allNodes.getOrDefault(key, new Node(newRow, newCol));
                
                if (tentativeG < neighbor.g) {
                    neighbor.g = tentativeG;
                    neighbor.h = heuristic(newRow, newCol, endRow, endCol);
                    neighbor.f = neighbor.g + neighbor.h;
                    neighbor.parent = current;

                     // Nếu neighbor đã tồn tại trong openSet, remove nó trước khi thêm lại
                    // if (openSet.contains(neighbor)) {
                    //     openSet.remove(neighbor);
                    // }
                    openSet.add(neighbor);
                    allNodes.put(key, neighbor);
                }
            }
        }

        // Không tìm được đường đi đến end ⇒ trả về đường đi gần nhất
        return reconstructPath(closest);
    }

    private static double heuristic(int r1, int c1, int r2, int c2) {
        // Dùng khoảng cách Euclidean cho di chuyển chéo
        return Math.hypot(r1 - r2, c1 - c2);
    }

    private static boolean isValid(int row, int col, boolean[][] grid) {
        return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length;
    }

    private static String key(int row, int col) {
        return row + "," + col;
    }

    private static List<Coordinate> reconstructPath(Node node) {
        LinkedList<Coordinate> path = new LinkedList<>();
        while (node != null) {
            path.addFirst(new Coordinate(node.row, node.col));
            node = node.parent;
        }
        return path;
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
