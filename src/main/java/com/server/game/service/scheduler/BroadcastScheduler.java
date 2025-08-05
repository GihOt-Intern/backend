// package com.server.game.service.scheduler;

// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;

// import com.server.game.model.game.GameState;
// import com.server.game.service.gameState.GameStateService;
// import com.server.game.service.position.PositionBroadcastService;

// import lombok.AllArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Service
// @Slf4j
// @AllArgsConstructor
// public class BroadcastScheduler {
    
//     private PositionBroadcastService positionBroadcastService;
//     private GameStateService gameStateService;
    
//     /**
//      * High-frequency broadcasting loop - runs every game tick
//      * Handles position updates broadcasting to clients
//      */
//     @Scheduled(fixedDelayString = "${game.tick-interval-ms}") // 33ms ~ 30 FPS for responsive gameplay
//     public void broadcastLoop() {
//         for (GameState gameState : gameStateService.getAllActiveGameStates()) {
//             try {
//                 // Broadcast position updates to all players in the game
//                 positionBroadcastService.broadcastGamePositions(gameState);
                
//             } catch (Exception e) {
//                 log.error("Error in broadcast loop for game: {}", gameState.getGameId(), e);
//             }
//         }
//     }
// }
