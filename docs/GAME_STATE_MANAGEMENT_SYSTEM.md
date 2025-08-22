# Game State Management System Documentation

## Overview

The game state management system is a comprehensive solution for managing player health, extended game states, and real-time updates in the PvP game. This system is designed to be extensible and can handle various game state data beyond just health values.

## Core Components

### 1. GameStateService
**Location**: `com.server.game.service.GameStateService`

The main service for managing game state across all active games. It provides:
- Game initialization and cleanup
- Player state management
- Health operations (damage, healing, health updates)
- Game statistics and monitoring

**Key Methods**:
- `initializeGameState(String gameId)` - Initialize a new game
- `initializePlayerState(gameId, slot, championId, initialHP)` - Initialize a player
- `updatePlayerHealth(gameId, slot, newHP)` - Update player health
- `applyDamage(gameId, slot, damage)` - Apply damage to a player
- `healPlayer(gameId, slot, healAmount)` - Heal a player
- `getGameStatistics(gameId)` - Get comprehensive game statistics

### 2. PlayerGameState
**Location**: `com.server.game.service.gameState.PlayerGameState`

Represents the complete state of a single player including:
- **Basic Stats**: Health (current/max), slot, champion ID
- **Extended Stats**: Gold, troop count, level, experience
- **Combat States**: Skill cooldowns, invulnerability, armor, magic resistance
- **Tracking**: Last damage time, last action time, regeneration status

**Extensible Properties**:
```java
// Health system
private int currentHP;
private final int maxHP;

// Economy system
private int gold = 0;
private int troopCount = 0;

// Progression system
private int level = 1;
private int experience = 0;

// Combat system
private long lastSkillUseTime = 0;
private float skillCooldownDuration = 0.0f;
private boolean isInvulnerable = false;
private long invulnerabilityEndTime = 0;
private int armorPoints = 0;
private int magicResistance = 0;

// Activity tracking
private long lastDamageTime = 0;
private long lastActionTime = 0;
private boolean isRegenerating = false;
```

### 3. GameStateManager
**Location**: `com.server.game.service.gameState.GameStateManager`

Advanced game state manager providing high-level operations:
- Complete game initialization with multiple players
- Advanced damage processing (critical hits, armor, magic resistance)
- Game end detection and winner determination
- Player respawning with invulnerability
- Comprehensive game state snapshots

**Key Features**:
- **Advanced Damage System**: Supports magical vs physical damage, critical hits, armor/resistance
- **Invulnerability System**: Temporary invulnerability for respawns or special abilities
- **Game End Detection**: Automatically detect when games end (1 or 0 players alive)
- **Statistics Tracking**: Damage dealt, healing done, game duration

### 4. GameStateBroadcastService
**Location**: `com.server.game.service.gameState.GameStateBroadcastService`

Handles broadcasting game state updates to all players in a game:
- Health updates (damage/healing)
- Player respawn events
- Multi-target effects (AoE damage)
- Game state synchronization

### 5. HealthRegenerationService
**Location**: `com.server.game.service.gameState.HealthRegenerationService`

Manages automatic health regeneration for players:
- Configurable regeneration rates and cooldowns
- Regeneration only starts after no damage for specified time
- Broadcasts regeneration updates to all players

## Integration Points

### GameHandler Integration
**Location**: `com.server.game.netty.receiveMessageHandler.GameHandler`

The GameHandler now uses the enhanced system:
```java
// Get slot to champion mapping
Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);

// Build champion to initial HP mapping
Map<ChampionEnum, Integer> championInitialHPMap = new HashMap<>();
for (ChampionEnum championId : slot2ChampionId.values()) {
    Integer initialHP = championService.getInitialHP(championId);
    championInitialHPMap.put(championId, initialHP);
}

// Initialize comprehensive game state using GameStateManager
boolean initSuccess = gameStateManager.initializeGame(gameId, slot2ChampionId, championInitialHPMap);
```

### PvP System Integration
**Location**: `com.server.game.service.PvPService`

The PvP system now uses the broadcast service for health updates:
```java
// Updated to use GameStateBroadcastService
private void broadcastHealthUpdate(String gameId, short targetSlot, int damage, long timestamp) {
    gameStateBroadcastService.broadcastHealthUpdate(gameId, targetSlot, damage, timestamp);
}
```

## Usage Examples

### 1. Basic Health Management
```java
// Apply damage to a player
gameStateService.applyDamage("game123", (short) 1, 50);

// Heal a player
gameStateService.healPlayer("game123", (short) 1, 30);

// Check if player is alive
boolean alive = gameStateService.isPlayerAlive("game123", (short) 1);
```

### 2. Advanced Combat System
```java
// Apply damage with critical hit and magic resistance
gameStateManager.processAdvancedDamage("game123", (short) 1, 75, true, true, 0.2f);

// Apply temporary invulnerability (5 seconds)
gameStateManager.applyInvulnerability("game123", (short) 1, 5000);
```

### 3. Player Respawn
```java
// Respawn player with 50% health and 3 seconds invulnerability
gameStateManager.respawnPlayer("game123", (short) 1, 0.5f, 3000);
```

### 4. Game State Monitoring
```java
// Get comprehensive game state
GameStateManager.GameStateSnapshot snapshot = gameStateManager.getGameStateSnapshot("game123");

// Check if game has ended
boolean gameEnded = gameStateManager.isGameEnded("game123");
Short winner = gameStateManager.getGameWinner("game123");

// Get players with low health (below 25%)
List<Short> lowHealthPlayers = gameStateManager.getLowHealthPlayers("game123", 0.25f);
```

### 5. Broadcasting Updates
```java
// Broadcast health update
gameStateBroadcastService.broadcastHealthUpdate("game123", (short) 1, 50, timestamp);

// Broadcast healing
gameStateBroadcastService.broadcastHealingUpdate("game123", (short) 1, 25, timestamp);

// Broadcast AoE damage
Map<Short, Integer> aoeDamage = Map.of((short) 1, 30, (short) 2, 35);
gameStateBroadcastService.broadcastMultipleHealthUpdates("game123", aoeDamage, timestamp);
```

## Configuration

### Health Regeneration Settings
```java
private static final long NO_DAMAGE_COOLDOWN_MS = 5000; // 5 seconds before regeneration
private static final int REGENERATION_AMOUNT = 10; // HP per tick
private static final float REGENERATION_PERCENTAGE = 0.02f; // 2% of max HP per tick
```

### Game State Features
- **AFK Detection**: Players are marked AFK after 5 minutes of inactivity
- **Experience System**: 100 EXP per level
- **Invulnerability**: Temporary protection from damage
- **Combat Stats**: Tracking damage dealt, received, and healing done

## Future Extensions

The system is designed to be easily extensible. Here are some potential additions:

### 1. Resource Management
```java
// Add to PlayerGameState
private int wood = 0;
private int stone = 0;
private int food = 0;
```

### 2. Buff/Debuff System
```java
// Add to PlayerGameState
private Map<String, BuffEffect> activeBuffs = new ConcurrentHashMap<>();
private Map<String, DebuffEffect> activeDebuffs = new ConcurrentHashMap<>();
```

### 3. Equipment System
```java
// Add to PlayerGameState
private Equipment weapon;
private Equipment armor;
private Equipment accessory;
```

### 4. Territory Control
```java
// Add to GameStateService
private Map<String, TerritoryState> territories = new ConcurrentHashMap<>();
```

## Error Handling

The system includes comprehensive error handling:
- Null checks for all game state operations
- Logging for debugging and monitoring
- Graceful degradation when players/games don't exist
- Automatic cleanup of game state when games end

## Performance Considerations

- Uses `ConcurrentHashMap` for thread-safe operations
- Efficient broadcasting with channel validation
- Minimal object creation in hot paths
- Optional statistics tracking (can be disabled)

## Testing Recommendations

1. **Unit Tests**: Test individual components (PlayerGameState, GameStateService)
2. **Integration Tests**: Test the complete flow from damage to broadcast
3. **Load Tests**: Test with multiple concurrent games and players
4. **Edge Cases**: Test with extreme values, disconnections, and race conditions

This game state management system provides a solid foundation for a scalable PvP game with room for future enhancements and features.
