package com.server.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchmakingStatusResponse {
    private String status;
    private String matchId;
    private Object gameServer;
} 