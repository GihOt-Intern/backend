// package com.server.game.repository.redis;

// import java.util.Optional;

// import org.springframework.stereotype.Repository;

// import com.server.game.abstraction.RedisRepository;

// import io.netty.channel.Channel;

// @Repository
// public class ChannelRepository extends RedisRepository<Channel> {

//     @Override
//     protected String getPrefix() {
//         return "online";
//     }

//     public void setUserOnline(String userId, Channel channel) {
//         save(userId, channel);
//     }

//     public Optional<Channel> getUserOnline(String userId) {
//         return Optional.ofNullable(findById(userId, Channel.class));
//     }

//     public void removeUserOnline(String userId) {
//         delete(userId);
//     }

//     public boolean isUserOnline(String userId) {
//         return redisTemplate.hasKey(getPrefix() + ":" + userId);
//     }
// }