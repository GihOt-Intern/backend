package com.server.game.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;


@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ChampionEnum {
    MELEE_AXE(0),
    ASSASSIN_SWORD(1),
    MARKSMAN_CROSSBOW(2),
    MAGE_SCEPTER(3);

    private final int championId;

    public static ChampionEnum fromInt(int id) {
        for (ChampionEnum champion : values()) {
            if (champion.getChampionId() == id) {
                return champion;
            }
        }
        throw new IllegalArgumentException("Unknown ChampionId: " + id);
    }
}
