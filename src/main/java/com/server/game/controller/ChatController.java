// package com.server.game.controller;


// import jakarta.validation.Valid;
// import lombok.AccessLevel;
// import lombok.RequiredArgsConstructor;
// import lombok.experimental.FieldDefaults;

// import java.security.Principal;
// import java.util.Base64;
// import java.util.List;

// // import org.springframework.http.HttpStatus;
// import org.springframework.messaging.handler.annotation.MessageMapping;
// import org.springframework.messaging.handler.annotation.SendTo;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.stereotype.Controller;

// import com.server.game.apiResponse.ApiResponse;
// import com.server.game.dto.request.ChatRequest;
// import com.server.game.dto.request.GetChatPrivateHistoryRequest;
// import com.server.game.dto.response.ChatResponse;
// import com.server.game.mapper.ChatMessageMapper;
// import com.server.game.model.ChatMessage;
// import com.server.game.protobuf.ChatPrivateRequest;
// import com.server.game.protobuf.ChatPrivateResponse;
// import com.server.game.protobuf.ChatPrivateResponseList;
// import com.server.game.service.ChatService;
// import com.server.game.service.WSService;


// @Controller
// @RequiredArgsConstructor
// @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// public class ChatController {

//     WSService wsService;
//     ChatService chatMessageService;
//     ChatMessageMapper chatMessageMapper;
//     SimpMessagingTemplate simpMessageTemplate;


//     @MessageMapping("/chat")
//     @SendTo("/topic/messages")
//     public ApiResponse<ChatResponse> sendMessage(@Valid ChatRequest request) throws InterruptedException{
//         ChatMessage chatMessage = chatMessageService.addChatMessage(request);
//         ChatResponse chatResponse = chatMessageMapper.toChatResponse(chatMessage);
//         return new ApiResponse<>(200, "New message", chatResponse);
//     }

//     // @MessageMapping("/private-chat")
//     // @SendToUser("/topic/private-messages")
//     // public ApiResponse<ChatResponse> sendPrivateMessage(@Valid ChatRequest request, Principal principal)
//     //         throws InterruptedException {
//     //     ChatMessage chatMessage = chatMessageService.addChatMessage(request);
//     //     chatMessage.setContent("Private message to " + principal.getName() + ": " + chatMessage.getContent());
//     //     ChatResponse chatResponse = chatMessageMapper.toChatResponse(chatMessage);
//     //     return new ApiResponse<>(200, "New message", chatResponse);
//     // }

//     @MessageMapping("/private-chat")
//     public void sendPrivateMessage(ChatPrivateRequest request, Principal principal)
//             throws InterruptedException {
//         try {
//             if (!principal.getName().equals(request.getSenderId())) {
//                 wsService.sendNotificationToUser(principal.getName(),
//                         new ApiResponse<>(400, "Sender ID does not match", null));
//                 return;
//             }    

//             ChatPrivateResponse response = chatMessageService.makePrivateMessage(request);
//             System.out.println(response);
//             // send to receiver
//             simpMessageTemplate.convertAndSendToUser(
//                     response.getReceiverId(), "/queue/private-messages",
//                     new ApiResponse<>(200, "New message", response));
            
//             wsService.sendNotificationToUser(principal.getName(),
//                     new ApiResponse<>(200, "Message sent successfully", response));
//         } catch (Exception e) {
//             // send error response to sender
//             wsService.sendNotificationToUser(principal.getName(),
//                     new ApiResponse<>(500, "Failed to send message: " + e.getMessage(), null));
//             throw e;
//         }
//     }

//     @MessageMapping("/private-chat/history")
//     public void getPrivateChatHistory(@Valid GetChatPrivateHistoryRequest request, Principal principal) throws Exception {
//         try {
//             if (!principal.getName().equals(request.getSenderId())) {
//                 wsService.sendNotificationToUser(principal.getName(),
//                         new ApiResponse<>(400, "Sender ID does not match", null)); 
//                 return;
//             }

//             List<ChatPrivateResponse> response = chatMessageService.getPrivateChatHistory(request);       
//             ChatPrivateResponseList responseList = ChatPrivateResponseList.newBuilder()
//                     .addAllResponses(response)
//                     .build();
//             String base64 = Base64.getEncoder().encodeToString(responseList.toByteArray());
//             // System.out.println("Private chat history response: " + responseList);
//             simpMessageTemplate.convertAndSendToUser(
//                     principal.getName(), "/queue/private-chat/history-response",
//                     base64);
//                     // "HELLO");
//                     // new ApiResponse<>(HttpStatus.OK.value(), "Private chat history retrieved", response));
//         } catch (Exception e) {
//             wsService.sendNotificationToUser(principal.getName(),
//                     new ApiResponse<>(500, "Failed to retrieve private chat history: " + e.getMessage(), null));
//             throw e;
//         }
//     }
// }
