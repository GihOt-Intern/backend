package com.server.game.repository.mongo;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
