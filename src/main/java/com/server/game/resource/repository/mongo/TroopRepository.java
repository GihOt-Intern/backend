package com.server.game.resource.repository.mongo;

import com.server.game.resource.model.Troop;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TroopRepository extends MongoRepository<Troop, Short> {
    
    List<Troop> findByType(String type);
    
    List<Troop> findByRole(String role);
    
    Optional<Troop> findByName(String name);
}
