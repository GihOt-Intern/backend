# Game Scheduler Separation

The game scheduling system has been refactored to separate concerns and improve performance.

## Architecture Overview

### Old System (Deprecated)
- **GameScheduler**: Single scheduler handling all game systems
- Single thread processing both game logic and broadcasting
- Performance bottlenecks when one system is slow

### New System (Recommended)
- **BroadcastScheduler**: Handles position broadcasting (20 FPS)
- **GameLogicScheduler**: Handles game logic updates (20 FPS + slower loops)
- **GameCoordinator**: Unified interface for both schedulers

## Scheduler Details

### BroadcastScheduler
- **Frequency**: 50ms (20 FPS)
- **Purpose**: Position updates broadcasting
- **Features**:
  - High-frequency updates for smooth client experience
  - Isolated from game logic errors
  - Dedicated thread pool

### GameLogicScheduler
- **Main Loop**: 50ms (20 FPS) - Movement, combat
- **Slow Loop**: 200ms (5 FPS) - AI, resources
- **Background Loop**: 1000ms (1 FPS) - Cleanup, metrics
- **Purpose**: All game mechanics and logic
- **Features**:
  - Multiple update frequencies for different systems
  - Separate error handling
  - Dedicated thread pool

### GameCoordinator
- **Purpose**: Unified interface for both schedulers
- **Features**:
  - Single registration point for games
  - Coordinated lifecycle management
  - Game statistics across both systems

## Migration Guide

### Before (Deprecated)
```java
@Autowired
private GameScheduler gameScheduler;

// Register game
gameScheduler.registerGame(gameId);

// Update position
gameScheduler.updatePosition(gameId, slot, position, timestamp);

// Check if active
gameScheduler.isGameActive(gameId);

// Unregister game
gameScheduler.unregisterGame(gameId);
```

### After (Recommended)
```java
@Autowired
private GameCoordinator gameCoordinator;

// Register game (registers with both schedulers)
gameCoordinator.registerGame(gameId);

// Update position
gameCoordinator.updatePosition(gameId, slot, position, timestamp);

// Check if active
gameCoordinator.isGameActive(gameId);

// Unregister game (unregisters from both schedulers)
gameCoordinator.unregisterGame(gameId);
```

## Benefits

1. **Performance**: Parallel processing of broadcasting and game logic
2. **Error Isolation**: Broadcasting continues even if game logic fails
3. **Scalability**: Different thread pools for different workloads
4. **Monitoring**: Separate metrics for each system
5. **Flexibility**: Independent update frequencies

## Updated Files

- **New**: `BroadcastScheduler.java` - Position broadcasting
- **New**: `GameLogicScheduler.java` - Game logic updates
- **New**: `GameCoordinator.java` - Unified interface
- **Updated**: `GameScheduler.java` - Deprecated with delegation
- **Updated**: `GameHandler.java` - Uses GameCoordinator
- **Updated**: `TestGameHandler.java` - Uses GameCoordinator

## Configuration

The system uses Spring's `@Scheduled` annotation with separate thread pools:
- Broadcasting: 5 threads
- Game Logic: 10 threads
- Automatic load balancing

## Backward Compatibility

The old `GameScheduler` is still available but **deprecated**. It delegates all calls to the new `GameCoordinator` and will be removed in future versions.

**Migration is now complete for all core services:**
- ✅ `TestGameHandler` - Uses `GameCoordinator`
- ✅ `MoveService` - Updated to use `GameCoordinator` 
- ✅ `GameScheduler` - Deprecated, delegates to `GameCoordinator`

## Next Steps

1. **Remove deprecated `GameScheduler`** in next release cycle
2. **Update any remaining references** to use `GameCoordinator`
3. **Clean up scheduling documentation** to reflect final architecture
