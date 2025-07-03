package com.server.game.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Document(collection = "rooms")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room {
    @Id
    String id;
    String name;

    @DBRef
    User host;

    @DBRef
    Set<User> players = new HashSet<>();

    int maxPlayers = 2;

    RoomStatus status = RoomStatus.WAITING;
} 