package com.server.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JoinQueueResponse {
    private String status;
    private String message;
    private int estimatedWaitTime;
} 