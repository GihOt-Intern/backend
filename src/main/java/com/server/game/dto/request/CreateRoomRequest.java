package com.server.game.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRoomRequest {
    @NotBlank(message = "Room name cannot be blank")
    @Size(min = 3, max = 20, message = "Room name must be between 3 and 20 characters")
    String name;
} 