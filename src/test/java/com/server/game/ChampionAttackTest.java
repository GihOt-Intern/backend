package com.server.game;

import com.server.game.model.game.Champion;
import com.server.game.model.game.component.attackComponent.AttackContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.champion.ChampionService;
import com.server.game.util.ChampionEnum;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class ChampionAttackTest {


	@Autowired
	private ChampionService championService;


    @Test
    void test01() {
        Champion attacker = championService.getChampionById(ChampionEnum.ASSASSIN_SWORD);
		Champion target = championService.getChampionById(ChampionEnum.MELEE_AXE);


		AttackContext ctx = new AttackContext(
			"game123",
			(short) 1, null,
			(short) 2, null,
			attacker, // attacker entity
			new Vector2(0, 0), // target position
			target, // target entity
			System.currentTimeMillis(),
			System.currentTimeMillis(),
			null // extra data
		);

		attacker.performAttack(ctx);
    }
} 