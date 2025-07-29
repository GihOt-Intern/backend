package com.server.game.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.model.token.InvalidatedToken;

public interface InvalidatedTokenRepository extends MongoRepository<InvalidatedToken, String> {

}
