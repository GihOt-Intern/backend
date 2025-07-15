package com.server.game.service;

import com.server.game.exception.http.DataNotFoundException;
import com.server.game.model.Room;
import com.server.game.model.RoomStatus;
import com.server.game.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Random;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomRedisService {

    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String ROOM_IDS_KEY = "rooms:ids";
    private static final Duration ROOM_TTL = Duration.ofHours(1); // 1 hour TTL
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ROOM_ID_LENGTH = 5;
    private static final int MAX_ATTEMPTS = 100; // Prevent infinite loops

    RedisUtil redisUtil;
    private final Random random = new Random();

    /**
     * Generates a unique 5-character alphanumeric room ID
     * @return A unique room ID
     */
    private String generateUniqueRoomId() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder roomId = new StringBuilder();
            for (int i = 0; i < ROOM_ID_LENGTH; i++) {
                roomId.append(ALPHANUMERIC_CHARS.charAt(random.nextInt(ALPHANUMERIC_CHARS.length())));
            }
            String generatedId = roomId.toString();
            
            // Check if this ID already exists
            if (!existsById(generatedId)) {
                return generatedId;
            }
        }
        // If we can't find a unique ID after max attempts, fall back to UUID (truncated)
        return UUID.randomUUID().toString().substring(0, ROOM_ID_LENGTH).toUpperCase();
    }

    public Room save(Room room) {
        if (room.getId() == null) {
            room.setId(generateUniqueRoomId());
        }
        
        String roomKey = ROOM_KEY_PREFIX + room.getId();
        redisUtil.set(roomKey, room, ROOM_TTL);
        
        // Add room ID to the set of all room IDs
        redisUtil.sAdd(ROOM_IDS_KEY, room.getId());
        
        return room;
    }

    public Room findById(String id) {
        String roomKey = ROOM_KEY_PREFIX + id;
        try {
            return redisUtil.get(roomKey, Room.class);
        } catch (DataNotFoundException e) {
            throw new DataNotFoundException("Room with ID " + id + " not found");
        }
    }

    public List<Room> findByStatus(RoomStatus status) {
        List<Room> rooms = new ArrayList<>();
        Set<Object> roomIds = redisUtil.sMembers(ROOM_IDS_KEY);
        
        for (Object roomId : roomIds) {
            try {
                String roomKey = ROOM_KEY_PREFIX + roomId.toString();
                Room room = redisUtil.get(roomKey, Room.class);
                if (room.getStatus() == status) {
                    rooms.add(room);
                }
            } catch (DataNotFoundException e) {
                // Room might have expired, remove from set
                redisUtil.sRemove(ROOM_IDS_KEY, roomId);
            }
        }
        
        return rooms;
    }

    public void delete(Room room) {
        String roomKey = ROOM_KEY_PREFIX + room.getId();
        redisUtil.delete(roomKey);
        redisUtil.sRemove(ROOM_IDS_KEY, room.getId());
    }

    public void deleteById(String id) {
        String roomKey = ROOM_KEY_PREFIX + id;
        redisUtil.delete(roomKey);
        redisUtil.sRemove(ROOM_IDS_KEY, id);
    }

    public boolean existsById(String id) {
        String roomKey = ROOM_KEY_PREFIX + id;
        return redisUtil.hasKey(roomKey);
    }
} 