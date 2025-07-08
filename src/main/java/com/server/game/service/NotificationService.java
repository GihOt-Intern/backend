package com.server.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.game.netty.UserChannelRegistry;
import com.server.game.netty.messageObject.sendObject.MessageSend;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final ObjectMapper objectMapper;

    /**
     * Send match found notification to all players in a match
     * @param playerIds List of player IDs to notify
     * @param matchId The match ID
     * @param websocketUrl The WebSocket URL for the game
     */
    public void notifyMatchFound(List<String> playerIds, String matchId, String websocketUrl) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "MATCH_FOUND",
                "matchId", matchId,
                "websocketUrl", websocketUrl,
                "message", "Match found! Connecting to game server..."
            );
            
            String message = objectMapper.writeValueAsString(notification);
            sendToPlayers(playerIds, message);
            
            log.info("Sent match found notification to {} players for match: {}", playerIds.size(), matchId);
        } catch (Exception e) {
            log.error("Failed to send match found notification", e);
        }
    }

    /**
     * Send game started notification to all players in a room
     * @param playerIds List of player IDs to notify
     * @param roomId The room ID
     * @param websocketUrl The WebSocket URL for the game
     */
    public void notifyGameStarted(List<String> playerIds, String roomId, String websocketUrl) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "GAME_STARTED",
                "roomId", roomId,
                "websocketUrl", websocketUrl,
                "message", "Game started! Connecting to game server..."
            );
            
            String message = objectMapper.writeValueAsString(notification);
            sendToPlayers(playerIds, message);
            
            log.info("Sent game started notification to {} players for room: {}", playerIds.size(), roomId);
        } catch (Exception e) {
            log.error("Failed to send game started notification", e);
        }
    }

    /**
     * Send a message to multiple players via their WebSocket channels
     * @param playerIds List of player IDs to send message to
     * @param message The message to send
     */
    private void sendToPlayers(List<String> playerIds, String message) {
        MessageSend messageSend = new MessageSend(message);
        
        for (String playerId : playerIds) {
            Channel channel = UserChannelRegistry.getChannel(playerId);
            if (channel != null && channel.isActive()) {
                try {
                    channel.writeAndFlush(messageSend);
                    log.debug("Sent notification to player: {}", playerId);
                } catch (Exception e) {
                    log.error("Failed to send notification to player: {}", playerId, e);
                }
            } else {
                log.warn("Player {} is not connected via WebSocket", playerId);
            }
        }
    }

    /**
     * Send a message to a single player
     * @param playerId The player ID to send message to
     * @param message The message to send
     */
    public void sendToPlayer(String playerId, String message) {
        MessageSend messageSend = new MessageSend(message);
        Channel channel = UserChannelRegistry.getChannel(playerId);
        
        if (channel != null && channel.isActive()) {
            try {
                channel.writeAndFlush(messageSend);
                log.debug("Sent notification to player: {}", playerId);
            } catch (Exception e) {
                log.error("Failed to send notification to player: {}", playerId, e);
            }
        } else {
            log.warn("Player {} is not connected via WebSocket", playerId);
        }
    }
} 