package com.server.game.resource.service;

import com.server.game.resource.model.Troop;
import com.server.game.resource.repository.TroopRepository;
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
    
    TroopRepository troopRepository;

    public Troop getTroopById(TroopEnum troopEnum) {
        return troopRepository.findById(troopEnum.getTroopId()).orElseGet(() -> {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum.getTroopId() + " not found.");
            return null;
        });
    }

    public Troop getTroopByName(String name) {
        return troopRepository.findByName(name).orElseGet(() -> {
            System.out.println(">>> [Log in TroopService] Troop with name " + name + " not found.");
            return null;
        });
    }

    public List<Troop> getTroopsByType(String type) {
        return troopRepository.findByType(type);
    }

    public List<Troop> getTroopsByRole(String role) {
        return troopRepository.findByRole(role);
    }

    public List<Troop> getAllTroops() {
        return troopRepository.findAll();
    }

    public Integer getTroopInitialHP(TroopEnum troopEnum) {
        Troop troop = getTroopById(troopEnum);
        if (troop == null) {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum + " not found.");
            return null;
        }
        return troop.getInitialHP();
    }

    public Float getTroopMovementSpeed(TroopEnum troopEnum) {
        Troop troop = getTroopById(troopEnum);
        if (troop == null) {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum + " not found");
            return null;
        }
        return troop.getMoveSpeed();
    }

    public Float getTroopAttackRange(TroopEnum troopEnum) {
        Troop troop = getTroopById(troopEnum);
        if (troop == null) {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum + " not found.");
            return 1.0f; // Default range
        }
        return troop.getAttackRange();
    }

    public Float getTroopDetectionRange(TroopEnum troopEnum) {
        Troop troop = getTroopById(troopEnum);
        if (troop == null) {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum + " not found.");
            return 3.0f; // Default range
        }
        return troop.getDetectionRange();
    }

    public int getTroopCost(TroopEnum troopEnum) {
        Troop troop = getTroopById(troopEnum);
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
        Troop troop = getTroopById(troopEnum);
        if (troop == null) {
            return 50; // Default damage
        }
        
        int baseDamage = troop.getAttack();
        // Add some randomness (±20%)
        int variation = (int) (baseDamage * 0.2);
        return baseDamage + (int) (Math.random() * variation * 2) - variation;
    }
}
