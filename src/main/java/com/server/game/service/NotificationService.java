package com.server.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.MessageSend;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Send room update notification to all players in a room
     * @param roomId The room ID
     * @param notification The notification object
     */
    public void notifyRoomUpdate(String roomId, Object notification) {
        try {
            String message = objectMapper.writeValueAsString(notification);
            sendToRoom(roomId, message);
            
            log.info("Sent room update notification to room: {}", roomId);
        } catch (Exception e) {
            log.error("Failed to send room update notification", e);
        }
    }

    /**
     * Send player joined room notification
     * @param roomId The room ID
     * @param playerId The player ID who joined
     * @param playerName The player name
     */
    public void notifyPlayerJoinedRoom(String roomId, String playerId, String playerName) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "PLAYER_JOINED_ROOM",
                "roomId", roomId,
                "playerId", playerId,
                "playerName", playerName,
                "message", playerName + " joined the room"
            );
            
            notifyRoomUpdate(roomId, notification);
        } catch (Exception e) {
            log.error("Failed to send player joined room notification", e);
        }
    }

    /**
     * Send player left room notification
     * @param roomId The room ID
     * @param playerId The player ID who left
     * @param playerName The player name
     */
    public void notifyPlayerLeftRoom(String roomId, String playerId, String playerName) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "PLAYER_LEFT_ROOM",
                "roomId", roomId,
                "playerId", playerId,
                "playerName", playerName,
                "message", playerName + " left the room"
            );
            
            notifyRoomUpdate(roomId, notification);
        } catch (Exception e) {
            log.error("Failed to send player left room notification", e);
        }
    }

    /**
     * Send host changed notification
     * @param roomId The room ID
     * @param newHostId The new host ID
     * @param newHostName The new host name
     */
    public void notifyHostChanged(String roomId, String newHostId, String newHostName) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "HOST_CHANGED",
                "roomId", roomId,
                "newHostId", newHostId,
                "newHostName", newHostName,
                "message", newHostName + " is now the host"
            );
            
            notifyRoomUpdate(roomId, notification);
        } catch (Exception e) {
            log.error("Failed to send host changed notification", e);
        }
    }

    /**
     * Send room deleted notification
     * @param roomId The room ID
     */
    public void notifyRoomDeleted(String roomId) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "ROOM_DELETED",
                "roomId", roomId,
                "message", "Room has been deleted"
            );
            
            notifyRoomUpdate(roomId, notification);
        } catch (Exception e) {
            log.error("Failed to send room deleted notification", e);
        }
    }

    /**
     * Send a message to all players in a room via their WebSocket channels
     * @param roomId The room ID
     * @param message The message to send
     */
    private void sendToRoom(String roomId, String message) {
        MessageSend messageSend = new MessageSend(message);
        Set<Channel> channels = ChannelManager.getChannelsByRoomId(roomId);
        
        if (channels == null || channels.isEmpty()) {
            log.warn("No players connected to room: {}", roomId);
            return;
        }
        
        for (Channel channel : channels) {
            if (channel.isActive()) {
                try {
                    channel.writeAndFlush(messageSend);
                    log.debug("Sent notification to player in room: {}", roomId);
                } catch (Exception e) {
                    log.error("Failed to send notification to player in room: {}", roomId, e);
                }
            }
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
            Channel channel = ChannelManager.getChannelByUserId(playerId);
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
        Channel channel = ChannelManager.getChannelByUserId(playerId);

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

    public void notifyMatchFoundRawSocket(List<String> playerIds, String matchId) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "MATCH_FOUND",
                "matchId", matchId,
                "message", "Match found! Please connect to the game server."
            );
            String message = objectMapper.writeValueAsString(notification);
            sendToPlayers(playerIds, message);
            log.info("Sent match found notification (raw socket) to {} players for match: {}", playerIds.size(), matchId);
        } catch (Exception e) {
            log.error("Failed to send match found notification (raw socket)", e);
        }
    }
} 