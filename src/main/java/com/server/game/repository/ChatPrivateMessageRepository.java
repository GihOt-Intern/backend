package com.server.game.repository;

import java.util.List;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.model.ChatPrivateMessage;

public interface ChatPrivateMessageRepository extends MongoRepository<ChatPrivateMessage, String> {
    List<ChatPrivateMessage> findBySenderId(String sender);
    List<ChatPrivateMessage> findByReceiverId(String receiver);
    List<ChatPrivateMessage> findBySenderIdAndReceiverId(String sender, String receiver);
    // List<ChatPrivateMessage> findByReceiverIdAndSenderId(String receiver, String sender);
}
