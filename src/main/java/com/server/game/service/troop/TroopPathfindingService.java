package com.server.game.service.troop;

import com.server.game.model.game.GameState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.util.ThetaStarPathfinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service to handle troop pathfinding and movement
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TroopPathfindingService {

    // gameId -> Map<troopId, List<Vector2>> paths
    private final Map<String, Map<String, List<Vector2>>> troopPaths = new ConcurrentHashMap<>();
    
    /**
     * Calculate a path from the current position to the target position using ThetaStarPathfinder
     * @param gameState The game state
     * @param troop The troop instance
     * @param targetPosition The target position
     * @return A list of waypoints to follow
     */
    public List<Vector2> calculatePath(GameState gameState, TroopInstance2 troop, Vector2 targetPosition) {
        // Convert world positions to grid cells
        GridCell startCell = gameState.toGridCell(troop.getCurrentPosition());
        GridCell targetCell = gameState.toGridCell(targetPosition);
        
        // Get the walkable grid from the game state
        boolean[][] walkableGrid = gameState.getGameMapGrid().getGrid();
        
        // Use ThetaStarPathfinder to find a path
        List<GridCell> gridPath = ThetaStarPathfinder.findPath(walkableGrid, startCell, targetCell);
        
        if (gridPath.isEmpty()) {
            log.debug("No path found for troop {} to position {}", troop.getIdAString(), targetPosition);
            return Collections.emptyList();
        }
        
        // Convert grid cells back to world positions
        List<Vector2> worldPath = new ArrayList<>();
        for (GridCell cell : gridPath) {
            worldPath.add(gameState.toPosition(cell));
        }
        
        // Store the path for this troop
        String gameId = gameState.getGameId();
        troopPaths.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                .put(troop.getIdAString(), worldPath);
        
        log.debug("Path calculated for troop {}: {} waypoints", troop.getIdAString(), worldPath.size());
        return worldPath;
    }
    
    /**
     * Get the next position in the path for a troop
     * @param gameId The game ID
     * @param troopId The troop ID
     * @return The next position to move to, or null if no path exists
     */
    public Vector2 getNextWaypoint(String gameId, String troopId) {
        Map<String, List<Vector2>> gamePaths = troopPaths.get(gameId);
        if (gamePaths == null) {
            return null;
        }
        
        List<Vector2> path = gamePaths.get(troopId);
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // Return the first waypoint (will be removed after reaching it)
        return path.get(0);
    }
    
    /**
     * Mark a waypoint as reached, removing it from the path
     * @param gameId The game ID
     * @param troopId The troop ID
     */
    public void waypointReached(String gameId, String troopId) {
        Map<String, List<Vector2>> gamePaths = troopPaths.get(gameId);
        if (gamePaths == null) {
            return;
        }
        
        List<Vector2> path = gamePaths.get(troopId);
        if (path == null || path.isEmpty()) {
            return;
        }
        
        // Remove the first waypoint as it's been reached
        path.remove(0);
        
        // If path is now empty, remove it entirely
        if (path.isEmpty()) {
            gamePaths.remove(troopId);
            if (gamePaths.isEmpty()) {
                troopPaths.remove(gameId);
            }
        }
    }
    
    /**
     * Clear all paths for a game
     * @param gameId The game ID
     */
    public void clearGamePaths(String gameId) {
        troopPaths.remove(gameId);
    }
    
    /**
     * Clear the path for a specific troop
     * @param gameId The game ID
     * @param troopId The troop ID
     */
    public void clearTroopPath(String gameId, String troopId) {
        Map<String, List<Vector2>> gamePaths = troopPaths.get(gameId);
        if (gamePaths != null) {
            gamePaths.remove(troopId);
            if (gamePaths.isEmpty()) {
                troopPaths.remove(gameId);
            }
        }
    }
    
    /**
     * Generate a random point inside a rectangle defined by its corners
     * @param cornerA First corner
     * @param cornerB Second corner
     * @return A random point inside the rectangle
     */
    public Vector2 getRandomPointInRectangle(Vector2 cornerA, Vector2 cornerB) {
        float minX = Math.min(cornerA.x(), cornerB.x());
        float maxX = Math.max(cornerA.x(), cornerB.x());
        float minY = Math.min(cornerA.y(), cornerB.y());
        float maxY = Math.max(cornerA.y(), cornerB.y());
        
        float randomX = minX + ThreadLocalRandom.current().nextFloat() * (maxX - minX);
        float randomY = minY + ThreadLocalRandom.current().nextFloat() * (maxY - minY);
        
        return new Vector2(randomX, randomY);
    }
    
    /**
     * Generate a random point inside the rectangle formed by the four minion positions
     * @param minionPositions The four minion positions defining the rectangle corners
     * @return A random point inside the rectangle
     */
    public Vector2 getRandomPointInMinionRectangle(List<Vector2> minionPositions) {
        if (minionPositions == null || minionPositions.size() < 2) {
            return null;
        }
        
        // Find min and max x and y values
        float minX = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        
        for (Vector2 pos : minionPositions) {
            minX = Math.min(minX, pos.x());
            maxX = Math.max(maxX, pos.x());
            minY = Math.min(minY, pos.y());
            maxY = Math.max(maxY, pos.y());
        }
        
        // Generate a random point in the rectangle
        float randomX = minX + ThreadLocalRandom.current().nextFloat() * (maxX - minX);
        float randomY = minY + ThreadLocalRandom.current().nextFloat() * (maxY - minY);
        
        return new Vector2(randomX, randomY);
    }
}
