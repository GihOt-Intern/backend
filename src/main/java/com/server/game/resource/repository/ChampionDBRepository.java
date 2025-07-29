package com.server.game.resource.repository;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.model.ChampionDB;


public interface ChampionDBRepository extends MongoRepository<ChampionDB, Short> {
    Optional<ChampionDB> findByName(String name);
    boolean existsByName(String name);
}
