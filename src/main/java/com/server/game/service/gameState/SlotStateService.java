package com.server.game.service.gameState;

import org.springframework.stereotype.Service;

import com.server.game.model.game.SlotState;
import com.server.game.model.game.TroopInstance2;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlotStateService {
    public void addTroop(SlotState slotState, TroopInstance2 troop) {
        if (slotState == null || troop == null) {
            log.warn("SlotState or TroopInstance is null when adding troop");
            return;
        }

        slotState.addTroop(troop);
        log.info("Added troop {} to slot state {}", troop.getStringId(), slotState.getSlot());
    }
}
