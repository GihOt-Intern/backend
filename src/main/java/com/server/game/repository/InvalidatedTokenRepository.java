package com.server.game.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.model.InvalidatedToken;

public interface InvalidatedTokenRepository extends MongoRepository<InvalidatedToken, String> {

}
