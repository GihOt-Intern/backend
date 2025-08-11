package com.server.game.model.game;


import java.util.UUID;


import com.server.game.model.game.context.AttackContext;
import com.server.game.resource.model.SlotInfo.BurgDB;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;


@EqualsAndHashCode(callSuper=false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class Burg extends Building {


    public Burg(SlotState ownerSlot, GameState gameState, Integer hp, BurgDB burgDB) {
        super("burg_" + ownerSlot.getSlot() + UUID.randomUUID().toString(),
        ownerSlot, gameState, hp, burgDB.getId(), burgDB.getPosition());
    }

    @Override
    public boolean receiveAttack(AttackContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receiveAttack'");
    }

    @Override
    protected void handleDeath(Entity killer) {
        // TODO: put handle death logic of Burg here 
    }

}
