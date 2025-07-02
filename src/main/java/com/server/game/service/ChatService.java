// package com.server.game.service;

// import lombok.AccessLevel;
// import lombok.RequiredArgsConstructor;
// import lombok.experimental.FieldDefaults;

// import java.util.Comparator;
// import java.util.List;

// import org.springframework.stereotype.Service;

// import com.server.game.dto.request.ChatRequest;
// import com.server.game.dto.request.GetChatPrivateHistoryRequest;
// import com.server.game.mapper.ChatMessageMapper;
// import com.server.game.model.ChatMessage;
// import com.server.game.model.ChatPrivateMessage;
// import com.server.game.protobuf.ChatPrivateRequest;
// import com.server.game.protobuf.ChatPrivateResponse;
// import com.server.game.repository.ChatMessageRepository;
// import com.server.game.repository.ChatPrivateMessageRepository;

// @Service
// @RequiredArgsConstructor
// @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// public class ChatService {
//     ChatMessageRepository chatMessageRepository;
//     ChatMessageMapper chatMessageMapper;
//     UserService userService;
//     ChatPrivateMessageRepository chatPrivateMessageRepository;

//     public ChatMessage addChatMessage(ChatRequest request){
//         ChatMessage chatMessage = chatMessageMapper.toChatMessage(request);
//         chatMessageRepository.save(chatMessage);
//         return chatMessage;
//     }

//     // private boolean isYourself(String sender, String receiver) {
//     //     return sender.equals(receiver);
//     // }

//     public ChatPrivateResponse makePrivateMessage(ChatPrivateRequest request) {
//         String receiverId = request.getReceiverId();
//         if (!userService.isUserExist(receiverId)) {
//             throw new IllegalArgumentException("Receiver does not exist");
//         }
        
//         // if (isYourself(request.getSenderId(), receiverId)) {
//         //     throw new IllegalArgumentException("Cannot send a message to yourself");
//         // }
        
//         ChatPrivateMessage chatPrivateMessage = chatMessageMapper.toChatPrivateMessage(request);
//         chatPrivateMessageRepository.save(chatPrivateMessage);
//         String senderName = userService.getUserByIdInternal(request.getSenderId()).getName();
//         ChatPrivateResponse response = chatMessageMapper.toChatPrivateResponse(chatPrivateMessage, senderName);
//         return response; 
//     }

//     public List<ChatPrivateResponse> getPrivateChatHistory(GetChatPrivateHistoryRequest request) {
//         String senderId = request.getSenderId();
//         String receiverId = request.getReceiverId();
//         if (!userService.isUserExist(senderId) || !userService.isUserExist(receiverId)) {
//             throw new IllegalArgumentException("One of the users does not exist");
//         }

//         // get messages from sender to receiver
//         List<ChatPrivateMessage> history = chatPrivateMessageRepository.findBySenderIdAndReceiverId(senderId, receiverId);
//         if (!senderId.equals(receiverId)) {
//             // flip the order to get messages from receiver to sender
//             history.addAll(chatPrivateMessageRepository.findBySenderIdAndReceiverId(receiverId, senderId));
//         }

//         // sort messages by timestamp
//         history.sort(Comparator.comparing(ChatPrivateMessage::getTimestamp));

//         return history.stream()
//                 .map(message -> chatMessageMapper.toChatPrivateResponse(
//                     message, userService.getUserByIdInternal(message.getSenderId()).getName()))
//                 .toList();
//     }
// }