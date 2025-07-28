package com.server.game.service.goldGeneration;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.server.game.model.GameState;
import com.server.game.model.SlotState;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.pvp.HealthUpdateSend;
import com.server.game.service.GameStateService;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class GoldGenerationService {

    GameStateService gameStateService;

    public void generateGold(String gameId) {
        gameStateService.genGold(gameId);
    }
}