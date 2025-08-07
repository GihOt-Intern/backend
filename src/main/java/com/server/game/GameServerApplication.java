package com.server.game;

import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.server.game.util.RedisUtil;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class GameServerApplication {

	@Autowired
	private RedisUtil redisUtil;

	public static void main(String[] args) {
		SpringApplication.run(GameServerApplication.class, args);
	}

	@PostConstruct
    void started() {
		// Set the default timezone to Asia/Ho_Chi_Minh
		// to ensure consistent date handling across the application
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

		// try {
		// 	System.out.println(">>> Testing Redis Connection");
		// 	redisUtil.testConnection();
		// } catch (RedisConnectionFailureException e) {
		// 	System.err.println("Redis connection failed: " + e.getMessage());
		// 	System.exit(1);
		// } catch (Exception e) {
		// 	System.err.println("Unexpected error during Redis test: " + e.getMessage());
		// 	e.printStackTrace();
		// }
	}
}
