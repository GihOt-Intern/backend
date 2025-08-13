package com.server.game.service.move;

import org.springframework.stereotype.Service;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.attack.AttackService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class MoveService {

    private final AttackService attackService;
    
    /**
     * Đặt mục tiêu di chuyển mới cho người chơi
     */
    public void setMove(MoveContext ctx, boolean needStopAttack) {
        ctx.getMover().setMoveContext(ctx, false);
        // log.info("Setting move target for entity {} to position {} at timestamp {}",
            // ctx.getMover().getStringId(), ctx.getTargetPoint(), ctx.getTimestamp());

        if (needStopAttack) {
            attackService.setStopAttacking(ctx.getMover());
        }
    }

    public void setMove(Entity entity, Vector2 targetPoint, boolean needStopAttack) {
        GameState gameState = entity.getGameState();
        MoveContext ctx = new MoveContext(gameState, entity, targetPoint, System.currentTimeMillis());
        this.setMove(ctx, needStopAttack);
    }

    public void setStopMoving(Entity entity) {
        entity.setMoveContext(null, true);
    }

    /**
     * Cập nhật vị trí dựa trên thời gian và mục tiêu di chuyển
     * Được gọi mỗi lần trước khi broadcast vị trí
     */
    public void updatePositions(GameState gameState) {
        for (Entity entity : gameState.getEntities()) {
            this.updatePositionOf(entity);
        }
    }


    private void updatePositionOf(Entity entity) {
        entity.performMoveAndBroadcast();
    }

    /**
     * Clear all move targets for a specific game (e.g., when game ends)
     */
    public void clearGameMove(GameState gameState) {
        for (Entity entity : gameState.getEntities()) {
            entity.setMoveContext(null, true);
        }
        // log.info("Cleared all move targets for game {}", gameState.getGameId());
    }
}
