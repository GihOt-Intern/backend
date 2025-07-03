package com.server.game;

import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class MyappApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyappApplication.class, args);
	}

	@PostConstruct
    void started() {
		// Set the default timezone to Asia/Ho_Chi_Minh
		// to ensure consistent date handling across the application
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }


	// @Autowired
	// private RedisTemplate<String, String> redisTemplate;

	// @PostConstruct
	// public void testRedis() {
	// 	redisTemplate.opsForValue().set("test_key", "Hello Redis!");
	// 	String val = redisTemplate.opsForValue().get("test_key");
	// 	System.out.println("Giá trị đọc từ Redis: " + val);
	// }

}
