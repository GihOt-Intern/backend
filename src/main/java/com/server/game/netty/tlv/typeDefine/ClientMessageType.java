package com.server.game.netty.tlv.typeDefine;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ClientMessageType {
    AUTHENTICATION_RECEIVE((short) 1),
    MESSAGE_RECEIVE((short) 3),
    CHOOSE_CHAMPION_RECEIVE((short) 6),
    PLAYER_READY_RECEIVE((short) 8),
    POSITION_UPDATE_RECEIVE((short) 19),
    DISTANCE_RECEIVE((short) 999),
    TEST_GAME_START_ANNOUNCE((short) 2025),
    ;

    short type;

    public static ClientMessageType fromShort(short value) {
        for (ClientMessageType t : values()) {
            if (t.type == value) return t;
        }
        throw new IllegalArgumentException("Unknown ClientMessageType: " + value);
    }
}
