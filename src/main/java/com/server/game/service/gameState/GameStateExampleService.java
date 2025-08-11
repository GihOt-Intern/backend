// package com.server.game.service.gameState;

// import org.springframework.stereotype.Component;

// import com.server.game.factory.GameStateFactory;
// import com.server.game.model.game.GameState;
// import com.server.game.util.ChampionEnum;

// import lombok.AccessLevel;
// import lombok.AllArgsConstructor;
// import lombok.experimental.FieldDefaults;
// import lombok.extern.slf4j.Slf4j;

// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// /**
//  * Example service demonstrating usage of the enhanced game state management system
//  */
// @Slf4j
// @Component
// @AllArgsConstructor
// @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// public class GameStateExampleService {

//     GameStateService gameStateService;
//     GameStateManager gameStateManager;
//     GameStateBroadcastService gameStateBroadcastService;
//     GameStateFactory gameStateBuilder;

//     /**
//      * Example: Initialize a game with multiple players
//      */
//     public void exampleGameInitialization() {
//         String gameId = "example_game_001";
        
//         // Setup slot to champion mapping
//         Map<Short, ChampionEnum> slot2ChampionId = new HashMap<>();
//         slot2ChampionId.put((short) 0, ChampionEnum.MELEE_AXE);
//         slot2ChampionId.put((short) 1, ChampionEnum.MARKSMAN_CROSSBOW);
//         slot2ChampionId.put((short) 2, ChampionEnum.MAGE_SCEPTER);


//         GameState gameState = gameStateBuilder.createGameState(gameId, slot2ChampionId);

//         // Initialize the game
//         boolean success = gameStateManager.initializeGame(gameState);
        
//         if (success) {
//             log.info("Successfully initialized example game: {}", gameId);
//             log.info("Game statistics:\n{}", gameStateService.getGameStatistics(gameId));
//         } else {
//             log.error("Failed to initialize example game: {}", gameId);
//         }
//     }
    
//     /**
//      * Example: Simulate combat between players
//      */
//     public void exampleCombatSimulation() {
//         String gameId = "example_game_001";
//         long timestamp = System.currentTimeMillis();
        
//         log.info("=== Combat Simulation Starting ===");
        
//         // Player 0 (Melee Axe) attacks Player 1 (Marksman)
//         log.info("Player 0 attacks Player 1");
//         gameStateBroadcastService.broadcastHealthUpdate(gameId, (short) 1, 120, timestamp);
        
//         // Player 1 (Marksman) attacks Player 0
//         log.info("Player 1 attacks Player 0");
//         gameStateBroadcastService.broadcastHealthUpdate(gameId, (short) 0, 80, timestamp + 1000);
        
//         // Player 2 (Mage) casts AoE spell on both players
//         log.info("Player 2 casts AoE spell");
//         Map<Short, Integer> aoeDamage = new HashMap<>();
//         aoeDamage.put((short) 0, 90);
//         aoeDamage.put((short) 1, 90);
//         gameStateBroadcastService.broadcastMultipleHealthUpdates(gameId, aoeDamage, timestamp + 2000);
        
//         // Check game state after combat
//         log.info("=== Combat Results ===");
//         log.info("Game statistics:\n{}", gameStateService.getGameStatistics(gameId));
        
//         // Check for low health players
//         List<Short> lowHealthPlayers = gameStateManager.getLowHealthPlayers(gameId, 0.3f);
//         if (!lowHealthPlayers.isEmpty()) {
//             log.info("Players with low health (< 30%): {}", lowHealthPlayers);
//         }
        
//         // Check if game has ended
//         if (gameStateManager.isGameEnded(gameId)) {
//             Short winner = gameStateManager.getWinnerSlot(gameId);
//             log.info("Game has ended! Winner: Player {}", winner);
//         }
//     }
    
//     /**
//      * Example: Advanced combat features
//      */
//     public void exampleAdvancedCombat() {
//         String gameId = "example_game_001";
        
//         log.info("=== Advanced Combat Features ===");
        
//         // Apply advanced damage with critical hit and magic resistance
//         boolean critHit = gameStateManager.processAdvancedDamage(
//             gameId, (short) 1, 150, true, true, 0.25f
//         );
//         log.info("Advanced damage applied with crit chance: {}", critHit);
        
//         // Apply temporary invulnerability
//         gameStateManager.applyInvulnerability(gameId, (short) 0, 3000); // 3 seconds
//         log.info("Applied 3 seconds of invulnerability to Player 0");
        
//         // Try to damage invulnerable player (should be blocked)
//         gameStateManager.processAdvancedDamage(
//             gameId, (short) 0, 100, false, false, 0.0f
//         );
//         log.info("Tried to damage invulnerable player - should be blocked");
        
//         // Apply healing
//         gameStateBroadcastService.broadcastHealingUpdate(gameId, (short) 1, 200, System.currentTimeMillis());
//         log.info("Applied healing to Player 1");
//     }
    
//     /**
//      * Example: Player respawn system
//      */
//     public void exampleRespawnSystem() {
//         String gameId = "example_game_001";
        
//         log.info("=== Respawn System Example ===");
        
//         // Check for dead players
//         List<Short> deadPlayers = gameStateManager.getDeadSlotIds(gameId);
//         if (!deadPlayers.isEmpty()) {
//             Short playerToRespawn = deadPlayers.get(0);
            
//             // Respawn with 75% health and 5 seconds invulnerability
//             boolean respawned = gameStateManager.respawnPlayer(
//                 gameId, playerToRespawn, 0.75f, 5000
//             );
            
//             if (respawned) {
//                 log.info("Successfully respawned Player {} with 75% health", playerToRespawn);
//                 gameStateBroadcastService.broadcastPlayerRespawn(gameId, playerToRespawn, 5000);
//             } else {
//                 log.warn("Failed to respawn Player {}", playerToRespawn);
//             }
//         } else {
//             log.info("No dead players to respawn");
//         }
//     }
    
//     /**
//      * Example: Game state monitoring
//      */
//     public void exampleGameStateMonitoring() {
//         String gameId = "example_game_001";
        
//         log.info("=== Game State Monitoring ===");
        
//         // Get comprehensive game state snapshot
//         GameStateManager.GameStateSnapshot snapshot = gameStateManager.getGameStateSnapshot(gameId);
        
//         log.info("Game ID: {}", snapshot.gameId);
//         log.info("Alive players: {}", snapshot.alivePlayers);
//         log.info("Dead players: {}", snapshot.deadPlayers);
//         log.info("Total players: {}", snapshot.getTotalPlayers());
        
//         // Display individual player stats
//         snapshot.gameState.getSlotStates().forEach((slot, slotState) -> {
//             log.info("Player {}: {}", slot, slotState.getStatusSummary());
//         });
        
//         // // Check if any players are AFK (5 minute threshold)
//         // snapshot.playerStates.forEach((slot, playerState) -> {
//         //     if (playerState.isAFK(300000)) { // 5 minutes
//         //         log.warn("Player {} appears to be AFK", slot);
//         //     }
//         // });
//     }
    
//     /**
//      * Example: Game cleanup
//      */
//     public void exampleGameCleanup() {
//         String gameId = "example_game_001";
        
//         log.info("=== Game Cleanup ===");
        
//         // Clean up all game state
//         gameStateManager.cleanupGame(gameId);
        
//         log.info("Game {} has been cleaned up", gameId);
//     }
    
//     /**
//      * Run complete example workflow
//      */
//     public void runCompleteExample() {
//         log.info("Starting complete game state management example...");
        
//         try {
//             exampleGameInitialization();
//             Thread.sleep(1000);
            
//             exampleCombatSimulation();
//             Thread.sleep(1000);
            
//             exampleAdvancedCombat();
//             Thread.sleep(1000);
            
//             exampleRespawnSystem();
//             Thread.sleep(1000);
            
//             exampleGameStateMonitoring();
//             Thread.sleep(1000);
            
//             exampleGameCleanup();
            
//             log.info("Complete example finished successfully!");
            
//         } catch (InterruptedException e) {
//             log.error("Example interrupted", e);
//             Thread.currentThread().interrupt();
//         } catch (Exception e) {
//             log.error("Example failed with error", e);
//         }
//     }
// }
