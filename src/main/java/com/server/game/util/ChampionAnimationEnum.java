package com.server.game.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;


@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ChampionAnimationEnum {
    ATTACK_ANIMATION((short) 0),
    SKILL_ANIMATION((short) 1);

    private final short attackTypeId;

    public static ChampionAnimationEnum fromShort(short id) {
        for (ChampionAnimationEnum attackType : values()) {
            if (attackType.getAttackTypeId() == id) {
                System.out.println("AttackTypeEnum: " + attackType.name() + ", ID: " + attackType.getAttackTypeId());
                return attackType;
            }
        }
        throw new IllegalArgumentException("Unknown AttackTypeId: " + id);
    }
}
