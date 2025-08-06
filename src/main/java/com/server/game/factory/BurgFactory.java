package com.server.game.factory;

import com.server.game.model.game.Burg;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.resource.model.SlotInfo.BurgDB;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class BurgFactory {

    public Burg createBurg(GameState gameState, SlotState ownerSlot, BurgDB burgDB) {
        return new Burg(ownerSlot, gameState, gameState.getBurgsInitHP(), burgDB);
    }
}
