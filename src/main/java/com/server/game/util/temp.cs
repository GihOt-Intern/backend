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