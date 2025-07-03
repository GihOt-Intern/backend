package com.server.game.mapper;

import com.server.game.dto.response.PlayerResponse;
import com.server.game.dto.response.RoomResponse;
import com.server.game.model.Room;
import com.server.game.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    PlayerResponse toPlayerResponse(User user);

    @Mapping(source = "host", target = "host")
    @Mapping(source = "players", target = "players")
    RoomResponse toRoomResponse(Room room);
} 