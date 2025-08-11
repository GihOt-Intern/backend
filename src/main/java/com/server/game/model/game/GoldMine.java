package com.server.game.model.game;

import java.util.UUID;

import com.server.game.model.game.context.AttackContext;
import com.server.game.model.map.component.Vector2;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

// abstract class to represent an entity in the game
// (Champion, Troop, Tower, Burg)
@Getter
@Slf4j
public final class GoldMine extends Entity implements FixedPositionEntity {

    protected final int goldAmount;
    protected final Vector2 position;


    public GoldMine(GameState gameState, int goldAmount, Vector2 position) {
        super("gold_mine_" + UUID.randomUUID().toString(), gameState);
        this.goldAmount = goldAmount;
        this.position = position;
    }


    @Override
    public boolean receiveAttack(AttackContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receiveAttack'");
    }


    @Override
    protected void addAllComponents() {
        
    }

    
}
