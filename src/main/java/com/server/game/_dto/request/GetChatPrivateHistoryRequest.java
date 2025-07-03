package com.server.game._dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.Data;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetChatPrivateHistoryRequest {
    @NotBlank(message = "senderId cannot be blank")
    String senderId;
    @NotBlank(message = "receiverId cannot be blank")
    String receiverId;
}
