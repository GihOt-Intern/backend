using UnityEngine;
using System.Collections.Generic;
using System.Linq;

public class MinPriorityQueue<T> where T : System.IComparable<T>
{
    private readonly List<T> _elements = new List<T>();

    public int Count => _elements.Count;

    public void Enqueue(T item)
    {
        _elements.Add(item);
        int childIndex = _elements.Count - 1;
        while (childIndex > 0)
        {
            int parentIndex = (childIndex - 1) / 2;
            if (_elements[childIndex].CompareTo(_elements[parentIndex]) >= 0)
                break;

            (_elements[childIndex], _elements[parentIndex]) = (_elements[parentIndex], _elements[childIndex]);
            childIndex = parentIndex;
        }
    }

    public T Dequeue()
    {
        int lastIndex = _elements.Count - 1;
        T frontItem = _elements[0];
        _elements[0] = _elements[lastIndex];
        _elements.RemoveAt(lastIndex);

        lastIndex--;
        int parentIndex = 0;
        while (true)
        {
            int leftChildIndex = parentIndex * 2 + 1;
            if (leftChildIndex > lastIndex)
                break;

            int rightChildIndex = leftChildIndex + 1;
            int smallestChildIndex = leftChildIndex;

            if (rightChildIndex <= lastIndex && _elements[rightChildIndex].CompareTo(_elements[leftChildIndex]) < 0)
            {
                smallestChildIndex = rightChildIndex;
            }

            if (_elements[parentIndex].CompareTo(_elements[smallestChildIndex]) <= 0)
                break;

            (_elements[parentIndex], _elements[smallestChildIndex]) = (_elements[smallestChildIndex], _elements[parentIndex]);
            parentIndex = smallestChildIndex;
        }
        return frontItem;
    }
}

/// <summary>
/// Implementation of Theta* pathfinding algorithm,
/// which extends A* to create smoother, more natural paths
/// by using line-of-sight checks between non-adjacent nodes.
/// </summary>
public static class ThetaStarPathfinder
{
    // 8 directions for movement
    private static readonly int[][] DIRECTIONS = new int[][]
    {
        new int[] {-1, 0},  // up
        new int[] {1, 0},   // down
        new int[] {0, -1},  // left
        new int[] {0, 1},   // right
        new int[] {-1, -1}, // diagonal up left
        new int[] {-1, 1},  // diagonal up right
        new int[] {1, -1},  // diagonal down left
        new int[] {1, 1}    // diagonal down right
    };

    /// <summary>
    /// Find a path from start to end using Theta* algorithm
    /// </summary>
    public static List<Vector2Int> FindPath(bool[][] grid, Vector2Int start, Vector2Int end)
    {
        int rows = grid.Length;
        int cols = grid[0].Length;

        // Check if start point is valid
        if (!IsValid(start.x, start.y, grid) || !grid[start.x][start.y])
        {
            Debug.Log("Start point is invalid or unwalkable");
            return new List<Vector2Int>();
        }

        // Check if end point is valid
        if (!IsValid(end.x, end.y, grid) || !grid[end.x][end.y])
        {
            Debug.Log("End point is not walkable");
            Vector2Int closestWalkable = FindClosestWalkablePosition(grid, end);
            if (closestWalkable == new Vector2Int(-1, -1))
            {
                Debug.Log("No walkable position found near end point");
                return new List<Vector2Int>();
            }
            Debug.Log($"Using closest walkable position: ({closestWalkable.x}, {closestWalkable.y})");
            end = closestWalkable;
        }

        // Maximum nodes to explore to prevent excessive searching
        const int MAX_NODES_TO_EXPLORE = 2000;
        int nodesExplored = 0;

        // Distance limit - don't search paths too far away (Manhattan distance * factor)
        int distanceLimit = (Mathf.Abs(start.x - end.x) + Mathf.Abs(start.y - end.y)) * 3;
        distanceLimit = Mathf.Min(distanceLimit, rows * cols / 4); // Cap to 1/4 of map size

        var openSet = new MinPriorityQueue<Node>();
        var allNodes = new Dictionary<string, Node>();
        var visited = new bool[rows][];
        for (int i = 0; i < rows; i++)
        {
            visited[i] = new bool[cols];
        }

        Node startNode = new Node(start.x, start.y, null, 0, 
                                  Heuristic(start.x, start.y, end.x, end.y));
        openSet.Enqueue(startNode);
        allNodes[Key(start.x, start.y)] = startNode;

        Node closest = startNode;
        double closestDistance = Heuristic(start.x, start.y, end.x, end.y);

        while (openSet.Count > 0)
        {
            Node current = openSet.Dequeue();
            nodesExplored++;

            if (current.row == end.x && current.col == end.y)
            {
                Debug.Log($"Found path to end point after exploring {nodesExplored} nodes");
                return ReconstructPath(current);
            }

            visited[current.row][current.col] = true;

            // Optimization: Track closest node
            double currentDistance = Heuristic(current.row, current.col, end.x, end.y);
            if (currentDistance < closestDistance)
            {
                closest = current;
                closestDistance = currentDistance;
            }

            // Early termination check
            if (currentDistance > distanceLimit && nodesExplored > MAX_NODES_TO_EXPLORE)
            {
                Debug.Log("Stopping search due to distance limit or max nodes explored");
                break; // Stop if we exceed distance limit or max nodes explored
            }

            foreach (var dir in DIRECTIONS)
            {
                int newRow = current.row + dir[0];
                int newCol = current.col + dir[1];

                if (!IsValid(newRow, newCol, grid) || visited[newRow][newCol] || !grid[newRow][newCol])
                {
                    continue;
                }

                // Prevent corner cutting with diagonal movement
                if (dir[0] != 0 && dir[1] != 0)
                {
                    int checkRow1 = current.row + dir[0];
                    int checkCol1 = current.col;
                    int checkRow2 = current.row;
                    int checkCol2 = current.col + dir[1];

                    if (!IsValid(checkRow1, checkCol1, grid) || !IsValid(checkRow2, checkCol2, grid) ||
                        (!grid[checkRow1][checkCol1] && !grid[checkRow2][checkCol2]))
                    {
                        continue;
                    }
                }

                string key = Key(newRow, newCol);
                if (!allNodes.TryGetValue(key, out Node neighbor))
                {
                    neighbor = new Node(newRow, newCol);
                    allNodes[key] = neighbor;
                }

                // THETA* MODIFICATION: Try to connect from parent directly if line-of-sight exists
                double tentativeG;
                Node pathParent;

                if (current.parent != null && LineOfSight(current.parent.row, current.parent.col, newRow, newCol, grid))
                {
                    // Direct path from grandparent exists - use Theta* shortcut
                    double directCost = System.Math.Sqrt(
                        System.Math.Pow(current.parent.row - newRow, 2) + 
                        System.Math.Pow(current.parent.col - newCol, 2)
                    );
                    tentativeG = current.parent.g + directCost;
                    pathParent = current.parent;
                }
                else
                {
                    // No line-of-sight, use standard A* approach
                    double moveCost = (dir[0] != 0 && dir[1] != 0) ? System.Math.Sqrt(2) : 1.0;
                    tentativeG = current.g + moveCost;
                    pathParent = current;
                }

                if (tentativeG < neighbor.g)
                {
                    neighbor.g = tentativeG;
                    neighbor.h = Heuristic(newRow, newCol, end.x, end.y);
                    neighbor.f = neighbor.g + neighbor.h;
                    neighbor.parent = pathParent;

                    openSet.Enqueue(neighbor);
                }
            }
        }

        if (nodesExplored >= MAX_NODES_TO_EXPLORE)
        {
            Debug.Log($"Pathfinding stopped after exploring {nodesExplored} nodes, returning closest path");
        }

        // Return best path found if exact path not found
        return ReconstructPath(closest);
    }

    /// <summary>
    /// Find the closest walkable position to the given target
    /// </summary>
    private static Vector2Int FindClosestWalkablePosition(bool[][] grid, Vector2Int target)
    {
        int rows = grid.Length;
        int cols = grid[0].Length;
        
        // Use A* to find closest walkable position
        var openSet = new MinPriorityQueue<Node>();
        var allNodes = new Dictionary<string, Node>();
        var visited = new bool[rows][];
        for (int i = 0; i < rows; i++)
        {
            visited[i] = new bool[cols];
        }
        
        // Start from target position
        Node startNode = new Node(target.x, target.y, null, 0, 0);
        startNode.f = 0; // Initial f-value is 0
        openSet.Enqueue(startNode);
        allNodes[Key(target.x, target.y)] = startNode;
        
        int maxNodes = 1000; // Limit search to prevent excessive exploration
        int nodesExplored = 0;
        
        while (openSet.Count > 0 && nodesExplored < maxNodes)
        {
            Node current = openSet.Dequeue();
            nodesExplored++;
            
            // If current position is walkable, return it immediately
            if (IsValid(current.row, current.col, grid) && grid[current.row][current.col])
            {
                Debug.Log($"Found walkable position after exploring {nodesExplored} nodes: ({current.row},{current.col})");
                return new Vector2Int(current.row, current.col);
            }
            
            visited[current.row][current.col] = true;
            
            // Check all 8 directions
            foreach (var dir in DIRECTIONS)
            {
                int newRow = current.row + dir[0];
                int newCol = current.col + dir[1];
                
                if (!IsValid(newRow, newCol, grid) || visited[newRow][newCol])
                {
                    continue;
                }
                
                string key = Key(newRow, newCol);
                if (!allNodes.TryGetValue(key, out Node neighbor))
                {
                    neighbor = new Node(newRow, newCol);
                    allNodes[key] = neighbor;
                }
                
                // Calculate movement cost (diagonal is sqrt(2), cardinal is 1)
                double moveCost = (dir[0] != 0 && dir[1] != 0) ? System.Math.Sqrt(2) : 1.0;
                double tentativeG = current.g + moveCost;
                
                if (tentativeG < neighbor.g)
                {
                    neighbor.g = tentativeG;
                    
                    // For finding closest walkable position:
                    // - g = distance from target
                    // - h = 0 (we're not trying to reach a specific goal)
                    // - f = g (prioritize cells closer to target)
                    neighbor.h = 0;
                    neighbor.f = neighbor.g;
                    neighbor.parent = current;
                    
                    openSet.Enqueue(neighbor);
                }
            }
        }

        Debug.Log($"No walkable position found after exploring {nodesExplored} nodes");
        return new Vector2Int(-1, -1); // No walkable position found
    }

    /// <summary>
    /// Calculate heuristic (straight-line distance) between two points
    /// </summary>
    private static double Heuristic(int r1, int c1, int r2, int c2)
    {
        // Use Euclidean distance for diagonal movement
        return System.Math.Sqrt(System.Math.Pow(r1 - r2, 2) + System.Math.Pow(c1 - c2, 2));
    }

    /// <summary>
    /// Check if there is a direct line-of-sight between two points
    /// </summary>
    private static bool LineOfSight(int x0, int y0, int x1, int y1, bool[][] grid)
    {
        int dx = System.Math.Abs(x1 - x0);
        int dy = System.Math.Abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (x != x1 || y != y1)
        {
            if (!IsValid(x, y, grid) || !grid[x][y])
            {
                return false; // No path available
            }
            
            int err2 = err * 2;
            if (err2 > -dy)
            {
                err -= dy;
                x += sx;
            }
            if (err2 < dx)
            {
                err += dx;
                y += sy;
            }
        }
        return true; // Path is clear
    }

    /// <summary>
    /// Check if a grid position is valid
    /// </summary>
    private static bool IsValid(int row, int col, bool[][] grid)
    {
        return row >= 0 && row < grid.Length && col >= 0 && col < grid[0].Length;
    }

    /// <summary>
    /// Create a unique key for the dictionary based on row and column
    /// </summary>
    private static string Key(int row, int col)
    {
        return $"{row},{col}";
    }

    /// <summary>
    /// Reconstruct the path from end to start
    /// </summary>
    private static List<Vector2Int> ReconstructPath(Node node)
    {
        // Use LinkedList to efficiently add elements at the beginning
        var path = new LinkedList<Vector2Int>();
        while (node != null)
        {
            path.AddFirst(new Vector2Int(node.row, node.col));
            node = node.parent;
        }

        // Print path for debugging
        Debug.Log($"Path found: {string.Join(" -> ", path.Select(p => $"({p.x},{p.y})"))}");

        // Convert LinkedList to List
        return path.ToList();
    }
    
    /// <summary>
    /// Node class for pathfinding
    /// </summary>
    private class Node : System.IComparable<Node>
    {
        public int row, col;
        public double g = double.MaxValue;
        public double h = 0;
        public double f = double.MaxValue;
        public Node parent;

        public Node(int row, int col)
        {
            this.row = row;
            this.col = col;
        }

        public Node(int row, int col, Node parent, double g, double h)
        {
            this.row = row;
            this.col = col;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }

        public int CompareTo(Node other)
        {
            if (other == null) return 1;
            return this.f.CompareTo(other.f);
        }
    }
}