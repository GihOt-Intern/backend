package com.server.game;

import java.util.TimeZone;

// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.RedisConnectionFailureException;
// import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;


import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class GameServerApplication {

	// @Autowired
	// private RedisTemplate<String, Object> redisTemplate;

	public static void main(String[] args) {
		SpringApplication.run(GameServerApplication.class, args);
	}

	@PostConstruct
    void started() {
		// Set the default timezone to Asia/Ho_Chi_Minh
		// to ensure consistent date handling across the application
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));


		try {
			System.out.println(">>> Call Redis");
			// redisTemplate.opsForValue().set("key", "value");
		} catch (RedisConnectionFailureException e) {
			System.err.println("Redis connection failed: " + e.getMessage());
			System.exit(1);
		}
	}
}
