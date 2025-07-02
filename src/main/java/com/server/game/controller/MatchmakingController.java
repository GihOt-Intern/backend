package com.server.game.controller;

import com.server.game.apiResponse.ApiResponse;
import com.server.game.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/matchmaking/queue")
@RequiredArgsConstructor
public class MatchmakingController {
    private final MatchmakingService matchmakingService;

    @PostMapping
    public ResponseEntity<?> joinQueue(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        String result = matchmakingService.joinQueue(userId);
        if ("ALREADY_IN_QUEUE".equals(result)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(409, "You are already in the matchmaking queue.", null));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(202, "You have been added to the matchmaking queue.",
                        Map.of("status", "SEARCHING", "estimatedWaitTime", matchmakingService.getEstimatedWaitTime())));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        String status = matchmakingService.getStatus(userId);
        if ("MATCH_FOUND".equals(status)) {
            Map<String, Object> matchInfo = matchmakingService.getMatchInfo(userId);
            return ResponseEntity.ok(Map.of(
                    "status", "MATCH_FOUND",
                    "matchId", matchInfo.get("matchId"),
                    "gameServer", matchInfo.get("gameServer")
            ));
        } else if ("SEARCHING".equals(status)) {
            return ResponseEntity.ok(Map.of("status", "SEARCHING"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "NOT_IN_QUEUE"));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> leaveQueue(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        matchmakingService.leaveQueue(userId);
        return ResponseEntity.ok(Map.of("message", "You have been removed from the matchmaking queue."));
    }
} 