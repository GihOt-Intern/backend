package com.server.game.service.gameState;

import org.springframework.stereotype.Service;

import com.server.game.model.game.SlotState;
import com.server.game.model.game.Troop;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlotStateService {
    public void addTroop(SlotState slotState, Troop troop) {
        if (slotState == null || troop == null) {
            log.warn("SlotState or TroopInstance is null when adding troop");
            return;
        }

        slotState.addTroop(troop);
        log.info("Added troop {} to slot state {}", troop.getStringId(), slotState.getSlot());
    }

    public boolean removeTroop(SlotState slotState, Troop troop) {
        if (slotState == null || troop == null) {
            log.warn("SlotState or TroopInstance is null when removing troop");
            return false;
        }

        boolean removed = slotState.getTroops().remove(troop);
        if (removed) {
            log.info("Removed troop {} from slot state {}", troop.getStringId(), slotState.getSlot());
        } else {
            log.warn("Failed to remove troop {} from slot state {} - troop not found", troop.getStringId(), slotState.getSlot());
        }
        return removed;
    }
}
