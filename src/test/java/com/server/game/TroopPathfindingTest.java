package com.server.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import com.server.game.config.SpringContextHolder;
import com.server.game.model.game.GameState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.game.TroopCreateContext;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.service.troop.TroopPathfindingService;
import com.server.game.util.TroopEnum;

public class TroopPathfindingTest {
    
    @Mock
    private GameState gameState;
    
    @Mock
    private TroopPathfindingService pathfindingService;
    
    @Mock
    private ApplicationContext applicationContext;
    
    @Mock
    private GameMapGrid gameMapGrid;
    
    private TroopInstance2 troop;
    private Vector2 startPosition = new Vector2(10f, 10f);
    private Vector2 targetPosition = new Vector2(20f, 20f);
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up Spring Context Holder with Mockito
        when(applicationContext.getBean(TroopPathfindingService.class)).thenReturn(pathfindingService);
        MockedStatic<SpringContextHolder> mockedStatic = mockStatic(SpringContextHolder.class);
        mockedStatic.when(() -> SpringContextHolder.getBean(TroopPathfindingService.class)).thenReturn(pathfindingService);
        
        // Set up game state and map grid
        when(gameState.getGameMapGrid()).thenReturn(gameMapGrid);
        when(gameState.getGameId()).thenReturn("test-game-id");
        
        // Coordinate conversion mocks
        GridCell startCell = new GridCell(1, 1);
        GridCell targetCell = new GridCell(2, 2);
        when(gameState.toGridCell(startPosition)).thenReturn(startCell);
        when(gameState.toGridCell(targetPosition)).thenReturn(targetCell);
        when(gameState.toPosition(startCell)).thenReturn(startPosition);
        when(gameState.toPosition(targetCell)).thenReturn(targetPosition);
        
        // Create mocked troop instance for testing
        TroopCreateContext createContext = mock(TroopCreateContext.class);
        when(createContext.getGameId()).thenReturn("test-game-id");
        when(createContext.getGameState()).thenReturn(gameState);
        when(createContext.getOwnerSlot()).thenReturn((short)1);
        when(createContext.getTroopEnum()).thenReturn(TroopEnum.AXIS);
        
        troop = new TroopInstance2(createContext);
        troop.setCurrentPosition(startPosition);
    }
    
    @Test
    public void testMoveTowardsUsingPathfinding() {
        // Set up path
        Vector2 waypoint1 = new Vector2(15f, 15f);
        Vector2 waypoint2 = targetPosition;
        List<Vector2> path = Arrays.asList(waypoint1, waypoint2);
        
        // Mock pathfinding service
        when(pathfindingService.getNextWaypoint("test-game-id", troop.getStringId()))
            .thenReturn(null) // First call, no existing waypoint
            .thenReturn(waypoint1); // Second call, after path calculation
        
        when(pathfindingService.calculatePath(gameState, troop, targetPosition))
            .thenReturn(path);
        
        // Set target position
        troop.setMoveTarget(targetPosition);
        
        // Move towards target
        troop.moveTowards(targetPosition, 2.0f, 0.1f);
        
        // Verify path was calculated
        verify(pathfindingService).calculatePath(gameState, troop, targetPosition);
        
        // Check that troop is moving towards first waypoint
        assertTrue(troop.isMoving());
        
        // Position should be updated towards waypoint1
        Vector2 currentPos = troop.getCurrentPosition();
        assertTrue(currentPos.x() > startPosition.x(), "Troop should have moved in X direction");
        assertTrue(currentPos.y() > startPosition.y(), "Troop should have moved in Y direction");
    }
    
    @Test
    public void testWaypointReached() {
        // Mock pathfinding service to return a waypoint that's very close to current position
        Vector2 closeWaypoint = new Vector2(10.1f, 10.1f); // Very close to start position
        when(pathfindingService.getNextWaypoint("test-game-id", troop.getStringId()))
            .thenReturn(closeWaypoint);
        
        // Move towards target
        troop.moveTowards(targetPosition, 2.0f, 0.1f);
        
        // Verify waypoint was marked as reached
        verify(pathfindingService).waypointReached("test-game-id", troop.getStringId());
        
        // Verify next waypoint was requested
        verify(pathfindingService, times(2)).getNextWaypoint("test-game-id", troop.getStringId());
    }
    
    @Test
    public void testSetMoveTarget() {
        // Set a target
        troop.setMoveTarget(targetPosition);
        
        // Verify path was cleared
        verify(pathfindingService).clearTroopPath("test-game-id", troop.getStringId());
        
        // Verify moving flag was set
        assertTrue(troop.isMoving());
        assertEquals(targetPosition, troop.getTargetPosition());
        
        // Set to null should stop movement
        troop.setMoveTarget(null);
        assertFalse(troop.isMoving());
        assertNull(troop.getTargetPosition());
    }
    
    @Test
    public void testDirectMovementFallbackWhenNoPath() {
        // Mock pathfinding service to return empty path
        when(pathfindingService.getNextWaypoint("test-game-id", troop.getStringId()))
            .thenReturn(null);
        when(pathfindingService.calculatePath(gameState, troop, targetPosition))
            .thenReturn(List.of());
            
        // Set start and target positions
        troop.setCurrentPosition(startPosition);
        troop.setMoveTarget(targetPosition);
        
        // Move towards target
        troop.moveTowards(targetPosition, 2.0f, 0.1f);
        
        // Verify position was updated using direct movement
        Vector2 currentPos = troop.getCurrentPosition();
        assertTrue(currentPos.x() > startPosition.x(), "Troop should have moved in X direction");
        assertTrue(currentPos.y() > startPosition.y(), "Troop should have moved in Y direction");
        
        // Verify path was calculated
        verify(pathfindingService).calculatePath(gameState, troop, targetPosition);
    }
}
