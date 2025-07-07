package com.server.game.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.server.game.model.Room;
import com.server.game.model.RoomStatus;

import java.util.List;

public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByStatus(RoomStatus status);
} 