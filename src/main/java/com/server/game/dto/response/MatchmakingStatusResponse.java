package com.server.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchmakingStatusResponse {
    private String status;
    private String matchId;
    private String websocketUrl;
} 