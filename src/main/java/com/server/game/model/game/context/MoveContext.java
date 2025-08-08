package com.server.game.model.game.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.lang.Nullable;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.util.ThetaStarPathfinder;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;


@Data
@Slf4j
public class MoveContext {
    
    @NotNull @Delegate
    private GameState gameState;
    @NotNull
    private Entity mover;
    @NotNull
    private Vector2 targetPoint;
    @NotNull
    private long timestamp;
    @Delegate @Nullable
    private PathComponent path = null;

    @NotNull
    private Map<Object, Object> extraData = new HashMap<>(); 
    

    public MoveContext(
        GameState gameState, Entity mover, Vector2 targetPoint, long timestamp) {
        this.gameState = gameState;
        this.mover = mover;
        this.targetPoint = targetPoint;
        this.timestamp = timestamp;
     
        this.setPath(this.findPath());
        log.info("Path found: {}", this.getPath().getPath().toString());
    }

    public void setPath(List<GridCell> path) {
        // if (path == null || path.isEmpty()) {
        //     this.path = null;
        //     return;
        // }
        
        this.path = new PathComponent(path);

        // TODO: try pop out the first cell
        if (this.path.hasNext()) { this.path.popCurrentCell(); }
    }

    public void addExtraData(Object key, Object value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value must not be null");
        }
        this.extraData.put(key, value);
    }

    public float getDistanceToTarget() {
        if (mover == null || targetPoint == null) {
            throw new IllegalStateException("Mover or target point is not set");
        }
        return mover.getCurrentPosition().distanceTo(targetPoint);
    }

    
    public List<GridCell> findPath() {
        GridCell startCell = gameState.toGridCell(mover.getCurrentPosition());
        GridCell targetCell = gameState.toGridCell(targetPoint);
        GameMapGrid gameMapGrid = gameState.getGameMapGrid();
        // log.info("Setting move target for entity {}: from {} to {}", mover.getStringId(), mover.getCurrentPosition(), targetPoint);
        // log.info("Calculating path for entity {} from cell {} to cell {}", mover.getStringId(), startCell, targetCell);

        List<GridCell> path = ThetaStarPathfinder.findPath(gameMapGrid, startCell, targetCell);
        if (path == null || path.isEmpty()) {
            log.warn("Pathfinding failed for entity {}:{} from {} to {}", 
                gameState.getGameId(), mover.getStringId(), startCell, targetCell);

            
            return new ArrayList<>(); // Return an empty path if pathfinding fails
        }

        return path;
    }
    


    //****** INNER CLASS *****//
    public static class PathComponent {
        @Getter
        private List<GridCell> path;
        private int index;

        public PathComponent(List<GridCell> path) {
            this.path = path;
            this.index = 0;
        }

        public int getIndex() {
            return index;
        }

        public int size() {
            return path.size();
        }

        public void clear() {
            this.index = this.path.size();
        }

        public boolean hasNext() {
            return index < path.size();
        }

        public GridCell peekCurrentCell() {
            if (!hasNext()) {
                System.out.println(">>> No more cells in path, returning null.");
                return null; // Hoặc có thể ném ngoại lệ nếu không có vị trí tiếp theo
            }
            return path.get(index);
        }
        
        public void popCurrentCell() {
            if (!hasNext()) {
                System.out.println(">>> No more cells in path, cannot pop.");
                // return null; // Hoặc có thể ném ngoại lệ nếu không có vị trí tiếp theo
            }
            ++index;
            // return path.get(index);
        }

        public GridCell getNextCell() {
            if (!hasNext()) {
                System.out.println(">>> No more cells in path, returning null.");
                return null; // Hoặc có thể ném ngoại lệ nếu không có vị trí tiếp theo
            }
            return path.get(index++);
        }
    }
    //****** END INNER CLASS *****//
}