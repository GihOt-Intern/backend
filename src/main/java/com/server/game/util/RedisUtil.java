package com.server.game.util;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.server.game.exception.http.DataNotFoundException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean isRedisReady() {
        try {
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                System.err.println("Redis connection factory is null");
                return false;
            }
            
            // Test the connection with a simple ping
            String pong = connectionFactory.getConnection().ping();
            boolean isReady = "PONG".equals(pong);
            
            if (isReady) {
                System.out.println("Redis connection is ready - PONG received");
            } else {
                System.err.println("Redis ping failed - received: " + pong);
            }
            
            return isReady;
        } catch (Exception e) {
            System.err.println("Redis connection failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Test Redis connection with detailed logging
     */
    public void testConnection() {
        System.out.println("=== Testing Redis Connection ===");
        try {
            // Test 1: Basic ping
            boolean ready = isRedisReady();
            System.out.println("Redis ready: " + ready);
            
            if (ready) {
                // Test 2: Set and get a test value
                String testKey = "connection_test";
                String testValue = "test_value_" + System.currentTimeMillis();
                
                set(testKey, testValue);
                Object retrieved = get(testKey);
                
                if (testValue.equals(retrieved)) {
                    System.out.println("Redis read/write test: PASSED");
                } else {
                    System.err.println("Redis read/write test: FAILED - Expected: " + testValue + ", Got: " + retrieved);
                }
                
                // Cleanup
                delete(testKey);
            }
        } catch (Exception e) {
            System.err.println("Redis connection test failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== Redis Connection Test Complete ===");
    }

    // ===== Key-Value =====
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public Object get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            throw new DataNotFoundException("Key not found in Redis: " + key);
        }
        return value;
    }

    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            throw new DataNotFoundException("Key not found in Redis: " + key);
        }
        
        if (!type.isInstance(value)) {
            throw new DataNotFoundException("Key found in Redis but type mismatch: " + key);
        }
        return type.cast(value);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, Duration ttl) {
        return redisTemplate.expire(key, ttl);
    }

    // ===== Hash (Map) =====
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public void hDel(String key, String... hashKeys) {
        redisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
    }

    // ===== List (Queue, Stack) =====
    public void lPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    // ===== Set (Unique elements) =====
    public void sAdd(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public void sRemove(String key, Object... values) {
        redisTemplate.opsForSet().remove(key, values);
    }
}

