package com.server.game.resource.repository.mongo;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.model.Champion;


public interface ChampionRepository extends MongoRepository<Champion, Short> {
    Optional<Champion> findByName(String name);
    boolean existsByName(String name);
}
