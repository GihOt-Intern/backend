// package com.server.game.ws;


// import org.springframework.lang.NonNull;
// import org.springframework.stereotype.Component;
// import org.springframework.web.socket.TextMessage;
// import org.springframework.web.socket.WebSocketSession;
// import org.springframework.web.socket.handler.TextWebSocketHandler;


// @Component
// public class TextSocketHandler extends TextWebSocketHandler {


    
//     @Override
//     public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
//         System.out.println("Client connected: " + session.getId());
//     }

    
//     @Override
//     protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
//         System.out.println("Received text message: " + message.getPayload());
//         // Handle text messages if needed
//     }
// }
