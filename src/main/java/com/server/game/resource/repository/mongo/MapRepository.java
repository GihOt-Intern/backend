package com.server.game.resource.repository.mongo;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.model.GameMap;


public interface MapRepository extends MongoRepository<GameMap, String> {
    Optional<GameMap> findByName(String name);
    boolean existsByName(String name);
}
