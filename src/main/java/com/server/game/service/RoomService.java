package com.server.game.service;

import com.server.game.dto.request.CreateRoomRequest;
import com.server.game.dto.response.RoomResponse;
import com.server.game.exception.DataNotFoundException;
import com.server.game.exception.IllegalArgumentException;
import com.server.game.mapper.RoomMapper;
import com.server.game.model.Room;
import com.server.game.model.RoomStatus;
import com.server.game.model.User;
import com.server.game.repository.RoomRepository;
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

    RoomRepository roomRepository;
    UserService userService;
    RoomMapper roomMapper;

    public RoomResponse createRoom(CreateRoomRequest request) {
        User host = userService.getUserInfo();

        Room room = new Room();
        room.setName(request.getName());
        room.setHost(host);
        room.getPlayers().add(host);
        room.setStatus(RoomStatus.WAITING);

        room = roomRepository.save(room);

        return roomMapper.toRoomResponse(room);
    }

    public List<RoomResponse> getAvailableRooms() {
        return roomRepository.findByStatus(RoomStatus.WAITING).stream()
                .map(roomMapper::toRoomResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse joinRoom(String roomId) {
        User user = userService.getUserInfo();
        Room room = getRoomById(roomId);

        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            throw new IllegalArgumentException("Room is full");
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("Game has already started in this room");
        }

        if (room.getPlayers().stream().anyMatch(p -> p.getId().equals(user.getId()))) {
            throw new IllegalArgumentException("User is already in the room");
        }

        room.getPlayers().add(user);
        room = roomRepository.save(room);

        return roomMapper.toRoomResponse(room);
    }

    public void leaveRoom(String roomId) {
        User user = userService.getUserInfo();
        Room room = getRoomById(roomId);

        if (room.getPlayers().stream().noneMatch(p -> p.getId().equals(user.getId()))) {
            throw new IllegalArgumentException("User is not in this room");
        }

        if (room.getHost().getId().equals(user.getId())) {
            roomRepository.delete(room);
        } else {
            room.getPlayers().removeIf(p -> p.getId().equals(user.getId()));
            roomRepository.save(room);
        }
    }
    
    public RoomResponse getRoomDetails(String roomId) {
        Room room = getRoomById(roomId);
        return roomMapper.toRoomResponse(room);
    }

    private Room getRoomById(String id) {
        return roomRepository.findById(id)
            .orElseThrow(() -> new DataNotFoundException("Room with ID " + id + " not found"));
    }
} 