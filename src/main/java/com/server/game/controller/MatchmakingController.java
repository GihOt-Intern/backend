package com.server.game.controller;

import com.server.game.apiResponse.ApiResponse;
import com.server.game.dto.response.JoinQueueResponse;
import com.server.game.dto.response.MatchmakingStatusResponse;
import com.server.game.service.matchmaking.MatchmakingService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/matchmaking/queue")
@RequiredArgsConstructor
public class MatchmakingController {
    private final MatchmakingService matchmakingService;

    @PostMapping
    public ResponseEntity<?> joinQueue(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String result = matchmakingService.joinQueue(userId);
        if ("ALREADY_IN_QUEUE".equals(result)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(409, "You are already in the matchmaking queue.", null));
        }
        // Should use a DTO for the response instead of a Map.of()
        JoinQueueResponse response = new JoinQueueResponse("SEARCHING", matchmakingService.getEstimatedWaitTime());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            new ApiResponse<>(202, "You have been added to the matchmaking queue.", response));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String status = matchmakingService.getStatus(userId);
        
        if ("MATCH_FOUND".equals(status)) {
            Map<String, Object> matchInfo = matchmakingService.getMatchInfo(userId);
            @SuppressWarnings("unchecked")
            Map<String, Object> gameServer = (Map<String, Object>) matchInfo.get("gameServer");
            String websocketUrl = (String) gameServer.get("websocketUrl");
            
            MatchmakingStatusResponse response = new MatchmakingStatusResponse(
                "MATCH_FOUND",
                (String) matchInfo.get("matchId"),
                websocketUrl
            );
            return ResponseEntity.ok(new ApiResponse<>(200, "Match found", response));
        }
        
        if ("SEARCHING".equals(status)) {
            MatchmakingStatusResponse response = new MatchmakingStatusResponse("SEARCHING", null, null);
            return ResponseEntity.ok(new ApiResponse<>(200, "Searching for match", response));
        }

        MatchmakingStatusResponse response = new MatchmakingStatusResponse("NOT_IN_QUEUE", null, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, "Not in queue", response));
    }

    @DeleteMapping
    public ResponseEntity<?> leaveQueue(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        matchmakingService.leaveQueue(userId);
        // return ResponseEntity.ok(Map.of("message", "You have been removed from the matchmaking queue."));
        return ResponseEntity.ok(new ApiResponse<>(200, "You have been removed from the matchmaking queue.", null));
    }
} 