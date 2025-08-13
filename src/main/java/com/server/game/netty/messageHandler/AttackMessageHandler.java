package com.server.game.netty.messageHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.factory.AttackContextFactory;
import com.server.game.model.game.GameState;
import com.server.game.model.game.Troop;
import com.server.game.model.game.context.AttackContext;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.attack.AttackReceive;
import com.server.game.service.attack.AttackService;
import com.server.game.service.gameState.GameStateService;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class AttackMessageHandler {
    GameStateService gameStateService;

    private final AttackContextFactory attackContextFactory;
    private final AttackService attackService;
    

    // Rate limiting: minimum time between position updates (in milliseconds)
    private static final long MIN_UPDATE_INTERVAL = 50; // 50ms = max 20 updates per second
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();


    @MessageMapping(AttackReceive.class)
    public void handleAttackMessage(AttackReceive receiveObject, Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        String entityStringId = receiveObject.getAttackerId();

        long clientTimestamp = receiveObject.getTimestamp();
        
        // Rate limiting check
        String playerKey = gameId + ":" + entityStringId;
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(playerKey);
        
        if (lastUpdate != null && (currentTime - lastUpdate) < MIN_UPDATE_INTERVAL) {
            log.debug("Rate limit exceeded for player: {} - ignore attack position update", playerKey);
            return;
        }

        // Update the last update time
        lastUpdateTime.put(playerKey, currentTime);

        if (entityStringId.equals(receiveObject.getTargetId())) {
            return;
        }

        if (entityStringId.startsWith("troop_") ) {
            GameState gameState = gameStateService.getGameStateById(gameId);
            Troop troop = (Troop) gameState.getEntityByStringId(entityStringId);
            troop.setInDefensiveStance(false);
            troop.setDefensePosition(null);
            log.info("Troop {} defensive stance disabled", entityStringId);
        }

        AttackContext attackContext = attackContextFactory.createAttackContext(
            gameId, entityStringId, receiveObject.getTargetId(), clientTimestamp);


        attackService.setAttack(attackContext);
        log.info("Attack context set for entity: {}", entityStringId);
    }
} 