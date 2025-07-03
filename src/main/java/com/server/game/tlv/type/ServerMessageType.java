package com.server.game.tlv.type;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ServerMessageType {
    DISTANCE_SEND((short) 2);


    short type;

    public static ServerMessageType fromShort(short value) {
        for (ServerMessageType t : values()) {
            if (t.type == value) return t;
        }
        throw new IllegalArgumentException("Unknown ServerMessageType: " + value);
    }
}
