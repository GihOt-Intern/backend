package com.server.game.dto.response;

import java.util.Date;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatPrivateResponse {
    String id;
    String senderName;
    String senderId;
    String receiverId;
    String content;
    Date timestamp;
}
