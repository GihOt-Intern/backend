package com.server.game.tlv.type;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ClientMessageType {
    DISTANCE_RECEIVE((short) 1);


    short type;

    public static ClientMessageType fromShort(short value) {
        for (ClientMessageType t : values()) {
            if (t.type == value) return t;
        }
        throw new IllegalArgumentException("Unknown ClientMessageType: " + value);
    }
}
