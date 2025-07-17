package com.server.game.service;

import com.server.game.dto.request.CreateRoomRequest;
import com.server.game.dto.response.RoomResponse;
import com.server.game.exception.http.DataNotFoundException;
import com.server.game.mapper.RoomMapper;
import com.server.game.model.Room;
import com.server.game.model.RoomStatus;
import com.server.game.model.RoomVisibility;
import com.server.game.model.User;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.InfoPlayersInRoomSend;
import com.server.game.netty.messageObject.sendObject.MessageSend;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;

// import com.server.game.exception.socket.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService {

    RoomRedisService roomRedisService;
    UserService userService;
    RoomMapper roomMapper;

    public RoomResponse createRoom(CreateRoomRequest request) {
        User host = userService.getUserInfo();

        Room room = new Room();
        room.setName(request.getName());
        room.setHost(host);
        room.getPlayers().add(host);
        room.setStatus(RoomStatus.WAITING);
        room.setMaxPlayers(request.getMaxPlayers());
        room.setPassword(request.getPassword());
        if (request.getPassword() != null) {
            room.setVisibility(RoomVisibility.LOCKED);
        } else {
            room.setVisibility(RoomVisibility.PUBLIC);
        }

        room = roomRedisService.save(room);

        return roomMapper.toRoomResponse(room);
    }

    public List<RoomResponse> getAvailableRooms() {
        return roomRedisService.findByStatus(RoomStatus.WAITING).stream()
                .filter(room -> room.getVisibility() == RoomVisibility.PUBLIC || room.getVisibility() == RoomVisibility.LOCKED)
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

        // Check password requirements
        if (room.getVisibility() == RoomVisibility.LOCKED) {
            if (password == null || !room.getPassword().equals(password)) {
                throw new IllegalArgumentException("Incorrect room password");
            }
        }

        room.getPlayers().add(user);
        room = roomRedisService.save(room);

        return roomMapper.toRoomResponse(room);
    }

    public void leaveRoom(String roomId) {
        User user = userService.getUserInfo();
        System.out.println(">>> [Log in leaveRoom()] Getting room by roomId: " + roomId); 
        Room room = getRoomById(roomId);
        System.out.println(">>> [Log in leaveRoom()] Room found.");
        Channel channel = ChannelManager.getChannelByUserId(user.getId());
        
        channel.writeAndFlush(new MessageSend(roomId)).addListener(future -> {
            if (future.isSuccess()) {
                ChannelManager.unregister(channel);
            }
        });


        if (room.getPlayers().stream().noneMatch(p -> p.getId().equals(user.getId()))) {
            throw new IllegalArgumentException("User is not in this room");
        }

        if (room.getHost().getId().equals(user.getId())) {
            // Host is leaving, transfer host role to another player if available
            if (room.getPlayers().size() <= 1) {
                // If no players left, delete the room
                roomRedisService.delete(room);
            } else {
                // Find a new host (first player in the list)
                User newHost = room.getPlayers().stream()
                        .filter(p -> !p.getId().equals(user.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No other players to transfer host role"));

                room.setHost(newHost);
                room.getPlayers().removeIf(p -> p.getId().equals(user.getId()));
                roomRedisService.save(room);
            }
        } else {
            // Regular player is leaving
            room.getPlayers().removeIf(p -> p.getId().equals(user.getId()));
            if (room.getPlayers().isEmpty()) {
                // No players left, delete the room
                roomRedisService.delete(room);
            } else {
                roomRedisService.save(room);
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

    // public RoomResponse startGame(String roomId) {
    //     User user = userService.getUserInfo();
    //     Room room = getRoomById(roomId);

    //     if (room == null) {
    //         throw new DataNotFoundException("Room not found");
    //     }

    //     if (!room.getHost().getId().equals(user.getId())) {
    //         throw new IllegalArgumentException("Only the host can start the game");
    //     }

    //     if (room.getStatus() != RoomStatus.WAITING) {
    //         throw new IllegalArgumentException("Game has already started in this room");
    //     }

    //     if (room.getPlayers().size() < 2) {
    //         throw new IllegalArgumentException("Need at least 2 players to start the game");
    //     }

    //     // Generate game server URL for WebSocket connection
    //     String gameServerUrl = generateGameServerUrl(roomId);
    //     room.setGameServerUrl(gameServerUrl);
    //     room.setStatus(RoomStatus.IN_GAME);
    //     room = roomRedisService.save(room);

    //     // Send WebSocket notification to all players in the room
    //     List<String> playerIds = room.getPlayers().stream()
    //             .map(User::getId)
    //             .toList();
    //     notificationService.notifyGameStarted(playerIds, roomId, gameServerUrl);

    //     return roomMapper.toRoomResponse(room);
    // }

    // private String generateGameServerUrl(String roomId) {
    //     // This should be configurable via application properties
    //     // For now, using a default WebSocket URL pattern
    //     return "ws://localhost:8386/game/" + roomId;
    // }

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
        Channel channel = ChannelManager.getChannelByUserId(user.getId());
        if (channel != null) {
            channel.writeAndFlush(new MessageSend(roomId)); // Send an empty message to notify the client
        }

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
        return roomMapper.toRoomResponse(room);
    }


    public void startGameSocket(String roomId) {
        User user = userService.getUserInfo(); // get channel who sending http request
        Channel channel = ChannelManager.getChannelByUserId(user.getId());

        Room room = getRoomById(roomId);

        if (room == null) {
            throw new DataNotFoundException("Room not found");
            // throw new SocketException("Room not found", channel);
        }

        if (!room.getHost().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only the host can start the game");
            // throw new SocketException("Only the host can start the game", channel);
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("Game has already started in this room");
            // throw new SocketException("Game has already started in this room", channel);
        }

        if (room.getPlayers().size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players to start the game");
            // throw new SocketException("Need at least 2 players to start the game", channel);
        }

        Set<Channel> channels = ChannelManager.getChannelsByGameId(roomId);
        if (channels.isEmpty()) {
            System.out.println("No active game channels found for room: " + roomId);
            return;
        }
  
        
        
        Map<Short, String> players = new HashMap<>();
        short slot = -1; 
        
        for (Channel ch : channels) {
            String username = ChannelManager.getUsernameByChannel(ch);
            ++slot;
            // Set slot for this channel and update the mapping
            ChannelManager.setSlot2Channel(slot, ch);
            players.put(slot, username);
        }
        
        InfoPlayersInRoomSend infoPlayerInRoomSend = new InfoPlayersInRoomSend(players);
        channel.writeAndFlush(infoPlayerInRoomSend);

        System.out.println(">>> Game started for room: " + roomId + " with " + (slot + 1) + " players");
        
        this.setRoomTo(room, RoomStatus.IN_GAME);
    }

    private void setRoomTo(Room room, RoomStatus status) {
        room.setStatus(status);
        roomRedisService.save(room);
    }
} 