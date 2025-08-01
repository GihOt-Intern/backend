package com.server.game.netty.tlv.messageEnum;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ReceiveMessageType {
    AUTHENTICATION_RECEIVE((short) 1),
    MESSAGE_RECEIVE((short) 3),
    LOBBY_LOADED_RECEIVE((short) -1),
    PING_RECEIVE((short) -3),
    CHOOSE_CHAMPION_RECEIVE((short) 6),
    PLAYER_READY_RECEIVE((short) 8),
    POSITION_UPDATE_RECEIVE((short) 19),
    CHAMPION_SKILL_CAST_RECEIVE((short) 24),
    
    ATTACK_RECEIVE((short) 100),


    @Deprecated
    CHAMPION_ATTACK_CHAMPION_RECEIVE((short) 1000),
    CHAMPION_ATTACK_TARGET_RECEIVE((short) 101),
    TARGET_ATTACK_CHAMPION_RECEIVE((short) 102),
    TARGET_ATTACK_TARGET_RECEIVE((short) 103),
    
    
    TROOP_SPAWN_RECEIVE((short) 200),
    DISTANCE_RECEIVE((short) 999),
    TEST_GAME_START_ANNOUNCE((short) 2025),
    ;

    short type;

    public static ReceiveMessageType fromShort(short value) {
        for (ReceiveMessageType t : values()) {
            if (t.type == value) return t;
        }
        throw new IllegalArgumentException("Unknown ClientMessageType: " + value);
    }
}
