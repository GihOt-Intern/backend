package com.server.game.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.Data;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetUserByEmailRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    String email;
}
