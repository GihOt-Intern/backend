package com.server.game.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum TroopEnum {
    AXIS((short) 0),
    SHADOW((short) 1),
    CROSSBAWL((short) 2),
    HEALER((short) 3);

    private final short troopId;

    public static TroopEnum fromShort(short id) {
        for (TroopEnum troop : values()) {
            if (troop.getTroopId() == id) {
                return troop;
            }
        }
        throw new IllegalArgumentException("Unknown TroopId: " + id);
    }
    
    public static TroopEnum fromString(String name) {
        for (TroopEnum troop : values()) {
            if (troop.name().equalsIgnoreCase(name)) {
                return troop;
            }
        }
        throw new IllegalArgumentException("Unknown Troop name: " + name);
    }
}
