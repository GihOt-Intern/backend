package com.server.game.resource.service;

import com.server.game.model.game.Troop;
import com.server.game.resource.model.TroopDB;
import com.server.game.resource.repository.TroopDBRepository;
import com.server.game.util.TroopEnum;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

import org.springframework.stereotype.Service;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class TroopService {
    
    TroopDBRepository troopDBRepository;

    public Troop getTroopById(TroopEnum troopEnum) {
        TroopDB troopDB = this.getTroopDBById(troopEnum);
        return new Troop(troopDB);
    }

    private TroopDB getTroopDBById(TroopEnum troopEnum) {
        return troopDBRepository.findById(troopEnum.getTroopId())
            .orElseThrow(() -> new IllegalArgumentException("Troop with id " + troopEnum.getTroopId() + " not found"));
    }

    public Troop getTroopByName(String name) {
        TroopDB troopDB = this.getTroopDBByName(name);
        return new Troop(troopDB);
    }

    private TroopDB getTroopDBByName(String name) {
        return troopDBRepository.findByName(name)
            .orElseThrow(() -> new IllegalArgumentException("Troop with name " + name + " not found"));
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

    
}
