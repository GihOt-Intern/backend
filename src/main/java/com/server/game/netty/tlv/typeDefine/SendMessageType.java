package com.server.game.netty.tlv.typeDefine;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum SendMessageType {
    ERROR_SEND((short) 0),
    AUTHENTICATION_SEND((short) 2),
    MESSAGE_SEND((short) 4),
    CHOOSE_CHAMPION_SEND((short) 7),
    INFO_PLAYERS_IN_ROOM_SEND((short) 5),
    PLAYER_READY_SEND((short) 9),
    INITIAL_POSITIONS_SEND((short) 11),
    CHAMPION_INITIAL_HPS_SEND((short) 12),
    CHAMPION_INITIAL_STATS_SEND((short) 13),
    POSITION_UPDATE_SEND((short) 20),
    ATTACK_ANIMATION_DISPLAY_SEND((short) 104),
    HEALTH_UPDATE_SEND((short) 105),
    DISTANCE_SEND((short) 998),
    TEST_GAME_START_RESPONSE((short) 2026),
    ;

    short type;

    public static SendMessageType fromShort(short value) {
        for (SendMessageType t : values()) {
            if (t.type == value) return t;
        }
        throw new IllegalArgumentException("Unknown ServerMessageType: " + value);
    }
}
