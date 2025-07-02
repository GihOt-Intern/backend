package com.server.game.model;

import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_private_messages")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatPrivateMessage {
    @Id
    String id;
    String senderId;
    String receiverId;
    String content;
    Date timestamp;
}


