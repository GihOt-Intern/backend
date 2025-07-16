package com.server.game.resource.repository.mongo;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.model.GameMap;


public interface GameMapRepository extends MongoRepository<GameMap, Short> {
    Optional<GameMap> findByName(String name);
    boolean existsByName(String name);
}
