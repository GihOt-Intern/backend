package com.server.game.service;

import com.server.game.exception.DataNotFoundException;
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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomRedisService {

    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String ROOM_IDS_KEY = "rooms:ids";
    private static final Duration ROOM_TTL = Duration.ofHours(1); // 1 hour TTL

    RedisUtil redisUtil;

    public Room save(Room room) {
        if (room.getId() == null) {
            room.setId(UUID.randomUUID().toString());
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

    public boolean existsById(String id) {
        String roomKey = ROOM_KEY_PREFIX + id;
        return redisUtil.hasKey(roomKey);
    }
} 