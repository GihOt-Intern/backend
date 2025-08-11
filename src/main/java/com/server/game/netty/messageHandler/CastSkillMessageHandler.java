package com.server.game.netty.messageHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.factory.CastSkillContextFactory;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.CastSkillReceive;
import com.server.game.service.castSkill.CastSkillService;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class CastSkillMessageHandler {

    private final CastSkillContextFactory castSkillContextFactory;
    private final CastSkillService castSkillService;


    // Rate limiting: minimum time between position updates (in milliseconds)
    private static final long MIN_UPDATE_INTERVAL = 50; // 50ms = max 20 updates per second
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();


    @MessageMapping(CastSkillReceive.class)
    public void handleCastSkillMessage(CastSkillReceive receiveObject, Channel channel) {
        String gameId = ChannelManager.getGameIdByChannel(channel);
        String entityStringId = receiveObject.getCasterId();

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

        CastSkillContext castSkillContext = castSkillContextFactory.createCastSkillContext(
            gameId, entityStringId, receiveObject.getTargetPosition(), clientTimestamp);


        log.info("Cast skill message received for entity {}: {}", entityStringId, castSkillContext);
        castSkillService.setCastSkill(castSkillContext);
    }
} 