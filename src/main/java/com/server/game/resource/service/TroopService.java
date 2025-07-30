package com.server.game.resource.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.game.resource.model.Troop;
import com.server.game.resource.repository.TroopRepository;
import com.server.game.util.TroopEnum;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TroopService {
    
    TroopRepository troopRepository;
    ObjectMapper objectMapper;

    public TroopService(TroopRepository troopRepository) {
        this.troopRepository = troopRepository;
        this.objectMapper = new ObjectMapper();
    }

    public Troop getTroopById(TroopEnum troopEnum) {
        return troopRepository.findById(troopEnum.getTroopId()).orElseGet(() -> {
            System.out.println(">>> [Log in TroopService] Troop with id " + troopEnum.getTroopId() + " not found in database. Trying to load from JSON...");
            return loadTroopFromJson(troopEnum);
        });
    }
    
    private Troop loadTroopFromJson(TroopEnum troopEnum) {
        try {
            String filename = troopEnum.name().toLowerCase() + ".json";
            ClassPathResource resource = new ClassPathResource("game/troop/" + filename);
            
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Troop troop = objectMapper.readValue(inputStream, Troop.class);
                    System.out.println(">>> [Log in TroopService] Successfully loaded troop " + troopEnum.name() + " from JSON file.");
                    return troop;
                }
            } else {
                System.out.println(">>> [Log in TroopService] JSON file for troop " + troopEnum.name() + " not found.");
                return null;
            }
        } catch (IOException e) {
            System.err.println(">>> [Error in TroopService] Failed to load troop from JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Troop getTroopByName(String name) {
        return troopRepository.findByName(name).orElseGet(() -> {
            System.out.println(">>> [Log in TroopService] Troop with name " + name + " not found in database. Trying to load from JSON...");
            try {
                // Try to match the name to a TroopEnum
                for (TroopEnum troopEnum : TroopEnum.values()) {
                    if (troopEnum.name().equalsIgnoreCase(name)) {
                        return loadTroopFromJson(troopEnum);
                    }
                }
                
                // If no enum match, try to load all JSON files and check names
                for (TroopEnum troopEnum : TroopEnum.values()) {
                    Troop troop = loadTroopFromJson(troopEnum);
                    if (troop != null && name.equalsIgnoreCase(troop.getName())) {
                        return troop;
                    }
                }
                
                System.out.println(">>> [Log in TroopService] Troop with name " + name + " not found in JSON files.");
                return null;
            } catch (Exception e) {
                System.err.println(">>> [Error in TroopService] Error loading troop by name: " + e.getMessage());
                return null;
            }
        });
    }

    public List<Troop> getTroopsByType(String type) {
        return troopRepository.findByType(type);
    }

    public List<Troop> getTroopsByRole(String role) {
        return troopRepository.findByRole(role);
    }

    public List<Troop> getAllTroops() {
        List<Troop> troops = troopRepository.findAll();
        
        // If database doesn't have any troops, load all from JSON
        if (troops == null || troops.isEmpty()) {
            System.out.println(">>> [Log in TroopService] No troops found in database. Loading from JSON files...");
            troops = new java.util.ArrayList<>();
            
            for (TroopEnum troopEnum : TroopEnum.values()) {
                Troop troop = loadTroopFromJson(troopEnum);
                if (troop != null) {
                    troops.add(troop);
                }
            }
        }
        
        return troops;
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
