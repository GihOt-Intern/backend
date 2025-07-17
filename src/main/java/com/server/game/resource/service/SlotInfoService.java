package com.server.game.resource.service;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


import org.springframework.stereotype.Service;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class SlotInfoService {
    

    // public Vector2 getInitialPosition(short slot) {

    //     Vector2 initialPosition = gameMapService.getInitialPosition(gameMapId, slot);
    //     if (initialPosition == null) {
    //         System.out.println(">>> [Log in ChampionService] Initial position for slot " + slot + " not found.");
    //     }
    //     return initialPosition;
    // }
}
