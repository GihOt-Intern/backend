package com.server.game.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.server.game.apiResponse.ApiResponse;
import com.server.game.dto.response.GetMyIdResponse;
import com.server.game.dto.response.GetUserResponse;
import com.server.game.service.WSService;


@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WSController {

    SimpMessagingTemplate simpMessageTemplate;
    WSService wsService;

    @MessageMapping("/myid")
    public void getMyId(Principal principal) throws Exception {
        try {
            Thread.sleep(500); 
            String userId = principal.getName();
            GetMyIdResponse response = new GetMyIdResponse(userId);
            simpMessageTemplate.convertAndSendToUser(
                    userId, "/queue/myid-response",
                    new ApiResponse<>(HttpStatus.OK.value(), "Get my ID successfully", response));
        } catch (Exception e) {
            simpMessageTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/myid-response",
                    new ApiResponse<>(500, "Error retrieving user ID", null));
            throw e;
        }
    }

    @MessageMapping("/u/all")
    public void getAllConnectedUsers(Principal principal) throws Exception {
        try {
            List<GetUserResponse> usersResponse = wsService.getAllUsers();
            System.out.println("All users: " + usersResponse);
            
            simpMessageTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/u/all-response",
                    new ApiResponse<>(HttpStatus.OK.value(), "Get all users successfully", usersResponse));
        } catch (Exception e) {
            simpMessageTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/u/all-response",
                    new ApiResponse<>(500, "Error retrieving all users", null));
            throw e;
        }        
    }

    

    
}
