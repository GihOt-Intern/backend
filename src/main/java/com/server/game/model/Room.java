package com.server.game.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    
    String id;
    String name;
    User host;
    Set<User> players = new HashSet<>();
    int maxPlayers = 2;
    RoomStatus status = RoomStatus.WAITING;
    RoomVisibility visibility = RoomVisibility.PUBLIC; // Default to public
    String password; // null or blank means public room
    String gameServerUrl; // WebSocket server URL for the game
} 