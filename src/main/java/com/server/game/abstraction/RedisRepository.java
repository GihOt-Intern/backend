// package com.server.game.abstraction;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.redis.core.RedisTemplate;

// public abstract class RedisRepository<T> {
    
//     @Autowired
//     protected RedisTemplate<String, Object> redisTemplate;

//     protected abstract String getPrefix();

//     private String makeKey(String id) {
//         return getPrefix() + ":" + id;
//     }

//     public void save(String id, T value) {
//         redisTemplate.opsForValue().set(makeKey(id), value);
//     }

//     public T findById(String id, Class<T> clazz) {
//         Object val = redisTemplate.opsForValue().get(makeKey(id));
//         return clazz.cast(val);
//     }

//     public void delete(String id) {
//         redisTemplate.delete(makeKey(id));
//     }
// }
