package com.server.game.repository;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
