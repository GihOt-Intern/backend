package com.server.game.controller;

import com.server.game.apiResponse.ApiResponse;
import com.server.game.dto.request.ChangeHostRequest;
import com.server.game.dto.request.CreateRoomRequest;
import com.server.game.dto.request.JoinRoomRequest;
import com.server.game.dto.response.RoomResponse;
import com.server.game.service.RoomService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {

    RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse roomResponse = roomService.createRoom(request);
        ApiResponse<RoomResponse> response = new ApiResponse<>(HttpStatus.CREATED.value(), "Room created successfully", roomResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRooms() {
        List<RoomResponse> rooms = roomService.getAvailableRooms();
        ApiResponse<List<RoomResponse>> response = new ApiResponse<>(HttpStatus.OK.value(), "Available rooms retrieved successfully", rooms);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomDetails(@PathVariable String roomId) {
        RoomResponse roomResponse = roomService.getRoomDetails(roomId);
        ApiResponse<RoomResponse> response = new ApiResponse<>(HttpStatus.OK.value(), "Room details retrieved successfully", roomResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoom(
        @PathVariable String roomId,
        @RequestBody(required = false) JoinRoomRequest joinRequest
    ) {
        String password = joinRequest != null ? joinRequest.getPassword() : null;
        RoomResponse roomResponse = roomService.joinRoom(roomId, password);
        ApiResponse<RoomResponse> response = new ApiResponse<>(HttpStatus.OK.value(), "Joined room successfully", roomResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/invite/{userId}")
    public ResponseEntity<ApiResponse<RoomResponse>> inviteUser(
        @PathVariable String roomId,
        @PathVariable String userId
    ) {
        RoomResponse roomResponse = roomService.inviteUser(roomId, userId);
        ApiResponse<RoomResponse> response = new ApiResponse<>(HttpStatus.OK.value(), "User invited successfully", roomResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(@PathVariable String roomId) {
        roomService.leaveRoom(roomId);
        ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), "Left room successfully", null);
        return ResponseEntity.ok(response);
    }

    // @PostMapping("/{roomId}/start")
    // public ResponseEntity<ApiResponse<RoomResponse>> startGame(@PathVariable String roomId) {
    //     RoomResponse roomResponse = roomService.startGame(roomId);
    //     ApiResponse<RoomResponse> response = new ApiResponse<>(HttpStatus.OK.value(), "Game started successfully", roomResponse);
    //     return ResponseEntity.ok(response);
    // }

    @PostMapping("/{roomId}/start")
    public void startGame(@PathVariable String roomId) {
        roomService.startGameSocket(roomId);
    }

    @PostMapping("/{roomId}/change-host")
    public ResponseEntity<ApiResponse<RoomResponse>> changeHost(
        @PathVariable String roomId,
        @Valid @RequestBody ChangeHostRequest request
    ) {
        RoomResponse roomResponse = roomService.changeHost(roomId, request.getNewHostId());
        ApiResponse<RoomResponse> response = new ApiResponse<>(HttpStatus.OK.value(), "Host changed successfully", roomResponse);
        return ResponseEntity.ok(response);
    }
} 