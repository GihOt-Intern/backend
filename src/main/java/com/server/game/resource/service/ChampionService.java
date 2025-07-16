package com.server.game.resource.service;

import com.server.game.resource.model.Champion;
import com.server.game.resource.repository.mongo.ChampionRepository;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ChampionService {
    ChampionRepository championRepository;

    public Champion getChampionById(short id) {
        return championRepository.findById(id).orElseGet(() -> {
            System.out.println(">>> [Log in ChampionService] Champion with id " + id + " not found.");
            return null;
        });
    }
}
