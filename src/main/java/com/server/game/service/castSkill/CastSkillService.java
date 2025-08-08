package com.server.game.service.castSkill;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.context.CastSkillContext;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CastSkillService {


    public void setCastSkill(CastSkillContext ctx) {
        Champion caster = ctx.getCaster();

        log.info("Set cast skill context for entity {}: {}", caster.getStringId(), ctx);
        
        caster.useSkill(ctx);
    }


    public void updateDurationSkills(GameState gameState) {
        // Only process cast skills for champions
        Set<Champion> champions = new HashSet<>(gameState.getChampions());
        for (Champion caster : champions) {
            this.processDurationSkillOf(caster);
        }
    }

    private void processDurationSkillOf(Champion caster) {
        // Perform the duration skill
        caster.updateDurationSkill();
    }

    // public void clearGameCastSkillContexts(GameState gameState) {
    //     Set<Champion> champions = new HashSet<>(gameState.getChampions());
    //     for (Champion caster : champions) {
    //         caster.setCastSkillContext(null);
    //     }
    // }
}
