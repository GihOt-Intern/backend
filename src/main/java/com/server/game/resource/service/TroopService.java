package com.server.game.resource.service;

import com.server.game.resource.model.TroopDB;
import com.server.game.resource.repository.TroopDBRepository;
import com.server.game.util.TroopEnum;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TroopService {
    
    // Store troop instance to create troopInstance2 instances
    private final Map<TroopEnum, TroopDB> troopDBCache = new HashMap<>();

    public TroopService(TroopDBRepository troopDBRepository) {
        // Preload all troopDBs into the cache
        List<TroopDB> allTroopDBs = troopDBRepository.findAll();
        for (TroopDB troopDB : allTroopDBs) {
            troopDBCache.put(TroopEnum.fromShort(troopDB.getId()), troopDB);
        }
    }

    public TroopDB getTroopDBById(TroopEnum troopEnum) {
        return troopDBCache.get(troopEnum);
    }

    public Set<TroopDB> getAllTroops() {
        return new HashSet<TroopDB>(troopDBCache.values());
    }
}
