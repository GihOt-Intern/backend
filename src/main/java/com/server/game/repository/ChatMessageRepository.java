package com.server.game.repository;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.model.ChatMessage;


public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    Optional<ChatMessage> findBySender(String sender);
}
