package com.server.game.resource.repository;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.model.TroopDB;


public interface TroopDBRepository extends MongoRepository<TroopDB, Short> {
    Optional<TroopDB> findByName(String name);
    boolean existsByName(String name);
}
