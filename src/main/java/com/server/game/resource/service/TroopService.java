package com.server.game.resource.service;

import com.server.game.model.game.TroopCreateContext;
import com.server.game.model.game.TroopInstance2;
import com.server.game.resource.model.TroopDB;
import com.server.game.resource.repository.TroopDBRepository;
import com.server.game.util.TroopEnum;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class TroopService {
    
    TroopDBRepository troopDBRepository;
    
    // Store troop instance to create troopInstance2 instances
    private final Map<TroopEnum, TroopDB> troopDBCache = new HashMap<>();

    public TroopService(TroopDBRepository troopDBRepository) {
        this.troopDBRepository = troopDBRepository;

        // Preload all troopDBs into the cache
        List<TroopDB> allTroopDBs = troopDBRepository.findAll();
        for (TroopDB troopDB : allTroopDBs) {
            troopDBCache.put(TroopEnum.fromShort(troopDB.getId()), troopDB);
        }
    }

    public TroopDB getTroopDBById(TroopEnum troopEnum) {
        return troopDBCache.get(troopEnum);
    }


    public List<TroopDB> getTroopsByType(String type) {
        return troopDBRepository.findByType(type);
    }

    public List<TroopDB> getTroopsByRole(String role) {
        return troopDBRepository.findByRole(role);
    }

    public List<TroopDB> getAllTroops() {
        return troopDBRepository.findAll();
    }

    public Integer getTroopInitialHP(TroopEnum troopEnum) {
        TroopDB troop = getTroopDBById(troopEnum);
        if (troop == null) {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum + " not found.");
            return null;
        }
        return troop.getInitialHP();
    }

    public Float getTroopMovementSpeed(TroopEnum troopEnum) {
        TroopDB troop = getTroopDBById(troopEnum);
        if (troop == null) {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum + " not found");
            return null;
        }
        return troop.getMoveSpeed();
    }

    public Float getTroopAttackRange(TroopEnum troopEnum) {
        TroopDB troop = getTroopDBById(troopEnum);
        if (troop == null) {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum + " not found.");
            return 1.0f; // Default range
        }
        return troop.getAttackRange();
    }

    public Float getTroopDetectionRange(TroopEnum troopEnum) {
        TroopDB troop = getTroopDBById(troopEnum);
        if (troop == null) {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum + " not found.");
            return 3.0f; // Default range
        }
        return troop.getDetectionRange();
    }

    public int getTroopCost(TroopEnum troopEnum) {
        TroopDB troop = getTroopDBById(troopEnum);
        if (troop == null) {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum + " not found.");
            return 100; // Default cost
        }
        return troop.getCost();
    }

    public boolean canPlayerAffordTroop(int playerGold, TroopEnum troopEnum) {
        int cost = getTroopCost(troopEnum);
        return playerGold >= cost;
    }

    public int calculateTroopDamage(TroopEnum troopEnum) {
        TroopDB troop = getTroopDBById(troopEnum);
        if (troop == null) {
            return 50; // Default damage
        }
        
        int baseDamage = troop.getAttack();
        // Add some randomness (±20%)
        int variation = (int) (baseDamage * 0.2);
        return baseDamage + (int) (Math.random() * variation * 2) - variation;
    }


    public TroopInstance2 createInstanceOf(TroopCreateContext ctx) {
        return new TroopInstance2(ctx);
    }
}
