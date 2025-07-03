package com.server.game.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.game.apiResponse.ApiResponse;
import com.server.game.dto.request.AuthenticationRequest;
import com.server.game.dto.request.IntrospectRequest;
import com.server.game.dto.request.LogoutRequest;
import com.server.game.dto.request.RefreshTokenRequest;
import com.server.game.dto.request.RegisterRequest;
import com.server.game.dto.response.AuthenticationResponse;
import com.server.game.dto.response.IntrospectResponse;
import com.server.game.dto.response.RefreshTokenResponse;
import com.server.game.dto.response.RegisterResponse;
import com.server.game.mapper.UserMapper;
import com.server.game.model.User;
import com.server.game.service.AuthenticationService;
import com.server.game.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;

// import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("${api.prefix}/auth")
public class AuthenticationController {

    AuthenticationService authenticationService;
    UserService userService;
    UserMapper userMapper;

    SimpMessagingTemplate messagingTemplate;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        RegisterResponse response = userMapper.toRegisterResponse(user);
        ApiResponse<RegisterResponse> apiResponse =
            new ApiResponse<>(HttpStatus.CREATED.value(), "User created successfully", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        User user = authenticationService.authenticate(request);
        String token = authenticationService.generateToken(user);
        AuthenticationResponse response = new AuthenticationResponse(user.getId(), user.getUsername(), "USER", token);
        ApiResponse<AuthenticationResponse> apiResponse =
            new ApiResponse<>(HttpStatus.OK.value(), "Authentication successful", response);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/introspect")
    public ResponseEntity<ApiResponse<IntrospectResponse>> introspect(@Valid @RequestBody IntrospectRequest request) {
        boolean isValid = authenticationService.introspect(request.getToken());
        IntrospectResponse response = new IntrospectResponse(isValid);
        ApiResponse<IntrospectResponse> apiResponse =
            new ApiResponse<>(HttpStatus.OK.value(), "Token introspection successful", response);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request.getToken());
        ApiResponse<Void> apiResponse =
            new ApiResponse<>(HttpStatus.OK.value(), "Logout successful", null);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String token = authenticationService.refreshToken(request.getToken());
        RefreshTokenResponse response = new RefreshTokenResponse(token);
        ApiResponse<RefreshTokenResponse> apiResponse =
            new ApiResponse<>(HttpStatus.OK.value(), "Token refreshed successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    // @MessageMapping("/login")
    // public void authenticateWebSocket(@Valid @Payload AuthenticationRequest request, 
    //                                 Principal principal) throws Exception {
    //     System.out.println("Received login via WS: " + request.getUsername());
    //     System.out.println("Principal = " + principal.getName());

    //     try {
    //         User user = authenticationService.authenticate(request);
    //         String token = authenticationService.generateToken(user);
    //         AuthenticationResponse response = authenticationMapper.toAuthenticationResponse(user, token);

    //         // Simulate sending a response back to the user
    //         messagingTemplate.convertAndSendToUser(
    //             principal.getName(),
    //             "/queue/login-response",
    //             new ApiResponse<>(HttpStatus.OK.value(), "WebSocket authentication successful", response)
    //         );
    //     } catch (Exception e) {
    //         System.out.println("WebSocket authentication failed: " + e.getMessage());
    //         // messagingTemplate.convertAndSendToUser(
    //         //     request.getEmail(),
    //         //     "/queue/login-response",
    //         //     new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "WebSocket authentication failed: " + e.getMessage(), null)
    //         // );
    //         messagingTemplate.convertAndSendToUser(
    //             principal.getName(),
    //             "/queue/login-response",
    //             new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "WebSocket authentication failed: " + e.getMessage(), null)
    //         );
    //         // throw new Exception("WebSocket authentication failed: " + e.getMessage());
    //     }
    // }


    // @EventListener
    // public void handleSessionConnectEvent(SessionConnectEvent event) {
    //     System.out.println("Session Connect Event");
    // }

    // @EventListener
    // public void handleSessionDisconnectEvent(SessionConnectEvent event) {
    //     System.out.println("Session Disconnect Event");

        
    // }
}
