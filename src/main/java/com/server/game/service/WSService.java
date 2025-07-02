// package com.server.game.service;

// import java.util.List;

// import org.springframework.messaging.simp.user.SimpUserRegistry;
// import org.springframework.stereotype.Service;

// import com.server.game.apiResponse.ApiResponse;
// import com.server.game.dto.response.GetUserResponse;
// import com.server.game.mapper.UserMapper;
// import com.server.game.model.User;

// import lombok.AccessLevel;
// import lombok.RequiredArgsConstructor;
// import lombok.experimental.FieldDefaults;
// import org.springframework.messaging.simp.SimpMessagingTemplate;


// @Service
// @RequiredArgsConstructor
// @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// public class WSService {

//     UserService userService;
//     UserMapper userMapper;
//     SimpMessagingTemplate simpMessageTemplate;
//     SimpUserRegistry simpUserRegistry;


//     // public List<String> getAllConnectedUsers() {
//     //     return simpUserRegistry.getUsers().stream()
//     //         .map(user -> user.getName())
//     //         .toList();
//     // }

//     public List<GetUserResponse> getAllUsers() {
//         List<User> users = userService.getAllUsers();
//         List<GetUserResponse> userResponses = users.stream()
//             .map(userMapper::toGetUserResponse)
//             .toList();
//         return userResponses;
//     }

//     public boolean isConnecting(String userId) {
//         if (simpUserRegistry.getUser(userId) == null)
//             throw new IllegalArgumentException("User not exist!");
//         return true;
//     }

//     public void sendNotificationToUser(String receiverId, ApiResponse<?> apiResponse) {
//         simpMessageTemplate.convertAndSendToUser(
//             receiverId, "/queue/notification",
//             apiResponse);
//     }
// }
