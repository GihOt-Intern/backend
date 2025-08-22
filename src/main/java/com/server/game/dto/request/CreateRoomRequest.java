package com.server.game.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.Min;

import com.server.game.model.room.RoomVisibility;

import jakarta.validation.constraints.Max;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRoomRequest {
    @NotBlank(message = "Room name cannot be blank")
    @Size(min = 3, max = 20, message = "Room name must be between 3 and 20 characters")
    String name;

    @Min(value = 2, message = "Minimum number of players is 2")
    @Max(value = 4, message = "Maximum number of players is 4")
    int maxPlayers;

    RoomVisibility visibility = RoomVisibility.PUBLIC; // Default to public
    String password; // Optional, can be blank
} 