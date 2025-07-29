package com.server.game.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Set;

import com.server.game.model.room.RoomStatus;
import com.server.game.model.room.RoomVisibility;

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