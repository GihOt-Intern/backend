package com.server.game.service;

import com.server.game.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchmakingService {
    private final RedisUtil redisUtil;
    private static final String QUEUE_KEY = "matchmaking:queue";
    private static final int MATCH_SIZE = 4;
    private static final int ESTIMATED_WAIT = 60;
    private static final String MATCH_KEY_PREFIX = "matchmaking:match:";
    private static final String MATCH_IDS_KEY = "matchmaking:ids";
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MATCH_ID_LENGTH = 5;
    private static final int MAX_ATTEMPTS = 100;
    private final Random random = new Random();

    public synchronized String joinQueue(String userId) {
        List<Object> queue = redisUtil.lRange(QUEUE_KEY, 0, -1);
        if (queue.contains(userId)) {
            return "ALREADY_IN_QUEUE";
        }
        redisUtil.lPush(QUEUE_KEY, userId);
        redisUtil.set(statusKey(userId), "SEARCHING");
        return "ADDED";
    }

    public String getStatus(String userId) {
        Object status = null;
        try {
            status = redisUtil.get(statusKey(userId));
        } catch (Exception ignored) {}
        return status == null ? "NOT_IN_QUEUE" : status.toString();
    }

    public void leaveQueue(String userId) {
        // Remove from queue
        List<Object> queue = redisUtil.lRange(QUEUE_KEY, 0, -1);
        queue.removeIf(u -> u.equals(userId));
        redisUtil.delete(QUEUE_KEY);
        for (Object u : queue) redisUtil.lPush(QUEUE_KEY, u);
        // Remove status
        redisUtil.delete(statusKey(userId));
        redisUtil.delete(matchKey(userId));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMatchInfo(String userId) {
        Object match = null;
        try {
            match = redisUtil.get(matchKey(userId));
        } catch (Exception ignored) {}
        if (match == null) return null;
        if (match instanceof Map) {
            Map<String, Object> matchMap = (Map<String, Object>) match;
            return matchMap;
        }
        return null;
    }

    @Scheduled(fixedDelay = 3000)
    public void matchPlayers() {

        if (!redisUtil.isRedisReady()) {
            System.err.println("Redis is not ready, skipping this matchmaking cycle.");
            return;
        }

        List<Object> queue = redisUtil.lRange(QUEUE_KEY, 0, -1);
        while (queue.size() >= MATCH_SIZE) {
            List<String> players = new ArrayList<>();
            for (int i = 0; i < MATCH_SIZE; i++) {
                players.add(queue.remove(queue.size() - 1).toString());
            }
            redisUtil.delete(QUEUE_KEY);
            for (Object u : queue) redisUtil.lPush(QUEUE_KEY, u);
            
            String matchId = generateUniqueMatchId();
            // String websocketUrl = generateGameServerUrl(matchId); // No longer needed
            
            // Notify all matched players with matchId, serverIp, serverPort
            for (String userId : players) {
                redisUtil.set(statusKey(userId), "MATCH_FOUND");
                // Clean up per-user match info after notification
                redisUtil.delete(matchKey(userId));
            }
            // Remove matchId from Redis set for uniqueness
            redisUtil.sRemove(MATCH_IDS_KEY, matchId);
        }
    }

    private String statusKey(String userId) {
        return "matchmaking:status:" + userId;
    }
    private String matchKey(String userId) {
        return MATCH_KEY_PREFIX + userId;
    }
    
    private String generateUniqueMatchId() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder matchId = new StringBuilder();
            for (int i = 0; i < MATCH_ID_LENGTH; i++) {
                matchId.append(ALPHANUMERIC_CHARS.charAt(random.nextInt(ALPHANUMERIC_CHARS.length())));
            }
            String generatedId = matchId.toString();
            // Check if this ID already exists
            if (!existsByMatchId(generatedId)) {
                return generatedId;
            }
        }
        // If we can't find a unique ID after max attempts, fall back to UUID (truncated)
        return UUID.randomUUID().toString().substring(0, MATCH_ID_LENGTH).toUpperCase();
    }

    private boolean existsByMatchId(String matchId) {
        String matchKey = MATCH_KEY_PREFIX + matchId;
        return redisUtil.hasKey(matchKey) || redisUtil.sMembers(MATCH_IDS_KEY).contains(matchId);
    }
    
    public int getEstimatedWaitTime() {
        return ESTIMATED_WAIT;
    }
} 