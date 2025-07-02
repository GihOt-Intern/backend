package com.server.game.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.server.game.exception.UnauthorizedException;
import com.server.game.service.AuthenticationService;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private AuthenticationService authenticationService;


    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Only process CONNECT messages
        if (accessor == null) {
            throw new IllegalArgumentException("STOMP headers are missing");
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                String userid = null;
                try {
                    userid = authenticationService.getJWTSubject(token);
                    accessor.setUser(new StompPrincipal(userid));
                    System.out.println(">>> User authenticated with ID: " + userid);
                }
                catch (UnauthorizedException e) {
                    System.out.println(">>> UnauthorizedException: " + e.getMessage());
                }
                catch (Exception e) {
                    System.out.println(">>> Exception during authentication: " + e.getMessage());
                    throw e;
                }            
            } else {
                throw new IllegalArgumentException("Invalid JWT Token");
            }
        }

        return message;
    }
}


