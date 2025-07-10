package com.server.game.service;

import com.server.game.dto.request.CreateRoomRequest;
import com.server.game.dto.response.RoomResponse;
import com.server.game.exception.DataNotFoundException;
import com.server.game.exception.IllegalArgumentException;
import com.server.game.mapper.RoomMapper;
import com.server.game.model.Room;
import com.server.game.model.RoomStatus;
import com.server.game.model.RoomVisibility;
import com.server.game.model.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService {

    RoomRedisService roomRedisService;
    UserService userService;
    RoomMapper roomMapper;
    NotificationService notificationService;

    public RoomResponse createRoom(CreateRoomRequest request) {
        User host = userService.getUserInfo();

        Room room = new Room();
        room.setName(request.getName());
        room.setHost(host);
        room.getPlayers().add(host);
        room.setStatus(RoomStatus.WAITING);
        room.setMaxPlayers(request.getMaxPlayers());
        room.setPassword(request.getPassword());
        if (request.getPassword() != null && request.getVisibility() != RoomVisibility.HIDDEN) {
            room.setVisibility(RoomVisibility.LOCKED);
        } else if (request.getPassword() == null && request.getVisibility() != RoomVisibility.HIDDEN) {
            room.setVisibility(RoomVisibility.PUBLIC);
        } else {
            room.setVisibility(RoomVisibility.HIDDEN);
        }

        room = roomRedisService.save(room);

        return roomMapper.toRoomResponse(room);
    }

    public List<RoomResponse> getAvailableRooms() {
        return roomRedisService.findByStatus(RoomStatus.WAITING).stream()
                .filter(room -> room.getVisibility() != RoomVisibility.HIDDEN) // Hide hidden rooms
                .map(roomMapper::toRoomResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse joinRoom(String roomId, String password) {
        User user = userService.getUserInfo();
        Room room = getRoomById(roomId);

        if (room == null) {
            throw new DataNotFoundException("Room not found");
        }

        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            throw new IllegalArgumentException("Room is full");
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("Game has already started in this room");
        }

        if (room.getPlayers().stream().anyMatch(p -> p.getId().equals(user.getId()))) {
            throw new IllegalArgumentException("User is already in the room");
        }

        // Check visibility and password requirements
        if (room.getVisibility() == RoomVisibility.LOCKED) {
            if (password == null || !room.getPassword().equals(password)) {
                throw new IllegalArgumentException("Incorrect room password");
            }
        } else if (room.getVisibility() == RoomVisibility.HIDDEN) {
            // Hidden rooms can only be joined by invitation (host adds players directly)
            throw new IllegalArgumentException("This room is hidden and requires invitation to join");
        }

        room.getPlayers().add(user);
        room = roomRedisService.save(room);

        // Notify other players in the room
        notificationService.notifyPlayerJoinedRoom(roomId, user.getId(), user.getUsername());

        return roomMapper.toRoomResponse(room);
    }

    public void leaveRoom(String roomId) {
        User user = userService.getUserInfo();
        Room room = getRoomById(roomId);

        if (room.getPlayers().stream().noneMatch(p -> p.getId().equals(user.getId()))) {
            throw new IllegalArgumentException("User is not in this room");
        }

        if (room.getHost().getId().equals(user.getId())) {
            // Host is leaving, delete the room
            notificationService.notifyRoomDeleted(roomId);
            roomRedisService.delete(room);
        } else {
            // Regular player is leaving
            room.getPlayers().removeIf(p -> p.getId().equals(user.getId()));
            if (room.getPlayers().isEmpty()) {
                // No players left, delete the room
                notificationService.notifyRoomDeleted(roomId);
                roomRedisService.delete(room);
            } else {
                roomRedisService.save(room);
                // Notify other players that this player left
                notificationService.notifyPlayerLeftRoom(roomId, user.getId(), user.getUsername());
            }
        }
    }
    
    public RoomResponse getRoomDetails(String roomId) {
        Room room = getRoomById(roomId);
        return roomMapper.toRoomResponse(room);
    }

    private Room getRoomById(String id) {
        return roomRedisService.findById(id);
    }

    public RoomResponse startGame(String roomId) {
        User user = userService.getUserInfo();
        Room room = getRoomById(roomId);

        if (room == null) {
            throw new DataNotFoundException("Room not found");
        }

        if (!room.getHost().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only the host can start the game");
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("Game has already started in this room");
        }

        if (room.getPlayers().size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players to start the game");
        }

        // Generate game server URL for WebSocket connection
        String gameServerUrl = generateGameServerUrl(roomId);
        room.setGameServerUrl(gameServerUrl);
        room.setStatus(RoomStatus.IN_GAME);
        room = roomRedisService.save(room);

        // Send WebSocket notification to all players in the room
        List<String> playerIds = room.getPlayers().stream()
                .map(User::getId)
                .toList();
        notificationService.notifyGameStarted(playerIds, roomId, gameServerUrl);

        return roomMapper.toRoomResponse(room);
    }

    private String generateGameServerUrl(String roomId) {
        // This should be configurable via application properties
        // For now, using a default WebSocket URL pattern
        return "ws://localhost:8386/game/" + roomId;
    }

    public RoomResponse changeHost(String roomId, String newHostId) {
        User user = userService.getUserInfo();
        Room room = getRoomById(roomId);

        if (room == null) {
            throw new DataNotFoundException("Room not found");
        }

        if (!room.getHost().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only the host can change host permission");
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("Cannot change host when game is in progress");
        }

        User newHost = userService.getUserByIdInternal(newHostId);
        if (!room.getPlayers().stream().anyMatch(p -> p.getId().equals(newHostId))) {
            throw new IllegalArgumentException("New host must be a player in the room");
        }

        room.setHost(newHost);
        room = roomRedisService.save(room);

        // Notify players about host change
        notificationService.notifyHostChanged(roomId, newHost.getId(), newHost.getUsername());

        return roomMapper.toRoomResponse(room);
    }

    public RoomResponse inviteUser(String roomId, String userId) {
        User host = userService.getUserInfo();
        Room room = getRoomById(roomId);

        if (room == null) {
            throw new DataNotFoundException("Room not found");
        }

        if (!room.getHost().getId().equals(host.getId())) {
            throw new IllegalArgumentException("Only the host can invite users");
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("Cannot invite users when game is in progress");
        }

        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            throw new IllegalArgumentException("Room is full");
        }

        User invitedUser = userService.getUserByIdInternal(userId);
        if (room.getPlayers().stream().anyMatch(p -> p.getId().equals(userId))) {
            throw new IllegalArgumentException("User is already in the room");
        }

        room.getPlayers().add(invitedUser);
        room = roomRedisService.save(room);

        // Notify other players about the invited user
        notificationService.notifyPlayerJoinedRoom(roomId, invitedUser.getId(), invitedUser.getUsername());

        return roomMapper.toRoomResponse(room);
    }
} 