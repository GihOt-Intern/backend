package com.server.game.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;


@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ChampionEnum {
    MELEE_AXE((short) 0),
    ASSASSIN_SWORD((short) 1),
    MARKSMAN_CROSSBOW((short) 2),
    MAGE_SCEPTER((short) 3);

    private final short championId;

    public static ChampionEnum fromShort(short id) {
        for (ChampionEnum champion : values()) {
            if (champion.getChampionId() == id) {
                return champion;
            }
        }
        throw new IllegalArgumentException("Unknown ChampionId: " + id);
    }
}
