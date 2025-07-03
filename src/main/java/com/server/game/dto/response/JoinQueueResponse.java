package com.server.game.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JoinQueueResponse {
    private String status;
    private int estimatedWaitTime;
} 