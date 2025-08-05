package com.server.game.service.move;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.service.attack.AttackService;
import com.server.game.service.gameState.GameCoordinator;
import com.server.game.service.position.PositionService;
import com.server.game.util.ThetaStarPathfinder;

import lombok.Data;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MoveService2 {

    AttackService attackService;
    

    /**
     * Đặt mục tiêu di chuyển mới cho người chơi
     */
    public void setMove(MoveContext ctx, boolean needStopAttack) {
        ctx.getMover().setMoveContext(ctx);
        log.info("Setting move target for entity {} to position {} at timestamp {}",
            ctx.getMover().getStringId(), ctx.getTargetPoint(), ctx.getTimestamp());

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
        entity.setMoveContext(null);
        log.info("Stopping move for entity {}", entity.getStringId());
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
            entity.setMoveContext(null);
        }
        log.info("Cleared all move targets for game {}", gameState.getGameId());
    }
}
