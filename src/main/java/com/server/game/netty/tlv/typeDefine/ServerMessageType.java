package com.server.game.netty.tlv.typeDefine;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ServerMessageType {
    AUTHENTICATION_SEND((short) 2),
    MESSAGE_SEND((short) 4),
    CHOOSE_CHAMPION_SEND((short) 6),
    INFO_PLAYERS_IN_ROOM_SEND((short) 12),
    DISTANCE_SEND((short) 18),
    ;

    short type;

    public static ServerMessageType fromShort(short value) {
        for (ServerMessageType t : values()) {
            if (t.type == value) return t;
        }
        throw new IllegalArgumentException("Unknown ServerMessageType: " + value);
    }
}
