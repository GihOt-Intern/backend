package com.server.game.dto.response;

import com.server.game.model.RoomStatus;
import com.server.game.model.RoomVisibility;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomResponse {
    String id;
    String name;
    PlayerResponse host;
    Set<PlayerResponse> players;
    int maxPlayers;
    RoomStatus status;
    RoomVisibility visibility;
    String gameServerUrl; // WebSocket server URL for the game
} 