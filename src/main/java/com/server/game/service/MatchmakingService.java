package com.server.game.service;

import com.server.game.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MatchmakingService {
    private final RedisUtil redisUtil;
    private static final String QUEUE_KEY = "matchmaking:queue";
    private static final int MATCH_SIZE = 4;
    private static final int ESTIMATED_WAIT = 60;

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

    public Map<String, Object> getMatchInfo(String userId) {
        Object match = null;
        try {
            match = redisUtil.get(matchKey(userId));
        } catch (Exception ignored) {}
        if (match == null) return null;
        return (Map<String, Object>) match;
    }

    @Scheduled(fixedDelay = 3000)
    public void matchPlayers() {
        List<Object> queue = redisUtil.lRange(QUEUE_KEY, 0, -1);
        while (queue.size() >= MATCH_SIZE) {
            List<String> players = new ArrayList<>();
            for (int i = 0; i < MATCH_SIZE; i++) {
                players.add(queue.remove(queue.size() - 1).toString());
            }
            redisUtil.delete(QUEUE_KEY);
            for (Object u : queue) redisUtil.lPush(QUEUE_KEY, u);
            String matchId = generateMatchId();
            Map<String, Object> matchInfo = new HashMap<>();
            matchInfo.put("matchId", matchId);
            matchInfo.put("gameServer", Collections.singletonMap("websocketUrl", "ws://123.45.67.89:8081/game"));
            for (String userId : players) {
                redisUtil.set(statusKey(userId), "MATCH_FOUND");
                redisUtil.set(matchKey(userId), matchInfo);
            }
        }
    }

    private String statusKey(String userId) {
        return "matchmaking:status:" + userId;
    }
    private String matchKey(String userId) {
        return "matchmaking:match:" + userId;
    }
    private String generateMatchId() {
        return "match-" + UUID.randomUUID();
    }
    public int getEstimatedWaitTime() {
        return ESTIMATED_WAIT;
    }
} 