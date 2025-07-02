package com.server.game.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.server.game._dto.request.CreateUserRequest;
import com.server.game._dto.request.GetUserByEmailRequest;
import com.server.game._dto.response.CreateUserResponse;
import com.server.game._dto.response.GetUserResponse;
import com.server.game.apiResponse.ApiResponse;
import com.server.game.mapper.UserMapper;
import com.server.game.model.User;
import com.server.game.service.UserService;

import org.springframework.http.HttpStatus;

import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("${api.prefix}/user")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;
    UserMapper userMapper;

    @GetMapping("/all")
    public  ResponseEntity<ApiResponse<List<GetUserResponse>>> getAllUsers() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println(authentication.getName() + " is accessing the user list");
        System.out.println("User roles: " + authentication.getAuthorities());

        List<User> users = userService.getAllUsers();
        List<GetUserResponse> usersResponse = users.stream()
                .map(userMapper::toGetUserResponse)
                .toList();
        ApiResponse<List<GetUserResponse>> response = new ApiResponse<>(200, "Users retrieved successfully", usersResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/myinfo")
    public ResponseEntity<ApiResponse<GetUserResponse>> getMyInfo() {
        User user = userService.getUserInfo();
        GetUserResponse userResponse = userMapper.toGetUserResponse(user);
        ApiResponse<GetUserResponse> response = new ApiResponse<>(HttpStatus.OK.value(), "User info retrieved successfully", userResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetUserResponse>> getUserById(@PathVariable String id) {
        User user = userService.getUserById(id);
        GetUserResponse userResponse = userMapper.toGetUserResponse(user);
        ApiResponse<GetUserResponse> response = new ApiResponse<>(HttpStatus.OK.value(), "User found", userResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<GetUserResponse>> getUserByEmail(@Valid @RequestBody GetUserByEmailRequest emailRequest) {
        User user = userService.getUserByEmail(emailRequest.getEmail());
        GetUserResponse userResponse = userMapper.toGetUserResponse(user);
        ApiResponse<GetUserResponse> response = new ApiResponse<>(HttpStatus.OK.value(), "User found", userResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        User user = userService.createUser(createUserRequest);
        CreateUserResponse createUserResponse = userMapper.toCreateUserResponse(user);
        ApiResponse<CreateUserResponse> response =
            new ApiResponse<>(HttpStatus.CREATED.value(), "User created successfully", createUserResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
