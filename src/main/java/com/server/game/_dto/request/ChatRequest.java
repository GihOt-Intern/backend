package com.server.game._dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatRequest {
    @NotBlank(message = "Sender is required")
    String sender;
    @NotBlank(message = "Content is required")
    String content;
}
