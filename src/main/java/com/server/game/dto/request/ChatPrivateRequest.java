package com.server.game.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatPrivateRequest {
    @NotBlank(message = "Sender ID is required")
    String senderId;
    @NotBlank(message = "Receiver is required")
    String receiverId;
    @NotBlank(message = "Content is required")
    String content;
}
