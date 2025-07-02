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
@Document(collection = "chat_messages")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {
    @Id
    String id;
    String sender;
    String content;
    Date timestamp;
}


