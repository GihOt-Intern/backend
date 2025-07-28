package com.server.game.resource.repository.mongo;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.model.GameMapGridCompress;


public interface GameMapGridCompressRepository extends MongoRepository<GameMapGridCompress, Short> {
    // Optional<GameMapGrid> findByName(String name);
    boolean existsByName(String name);
}
