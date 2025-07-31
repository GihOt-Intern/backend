package com.server.game.model.game.championSkill;

import java.util.Set;

import com.server.game.config.SpringContextHolder;
import com.server.game.model.game.Champion;
import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.component.attackComponent.SkillReceivable;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.Util;
import com.server.game.model.map.shape.CircleShape;

// Xoay rìu trong 5s, mỗi giây gây sát thương phạm vi xung quanh 1 ô = 40 + 20% DEF		
public class MeleeSkill extends SkillComponent {

    private static final float DURATION_SECONDS = 5.0f;
    private static final float DAMAGE_INTERVAL_SECONDS = 1.0f;
    private static final float DAMAGE_RADIUS = 5.0f;


    private long startTick = -1;                // Khi bắt đầu kích hoạt skill
    private long endTick = -1;                  // Khi kết thúc
    private long nextDamageTick = -1;           // Tick tiếp theo cần gây damage
    private boolean active = false;


    public float getDamagePerSecond() {
        return 40 + 0.2f * this.getSkillOwner().getDefense();
    }

    public MeleeSkill(Champion owner, ChampionAbility ability) {
        super(owner, ability);
    }

    @Override
    protected void doUse(CastSkillContext context) {
        long currentTick = context.getCurrentTick();
        
        this.startTick = currentTick;
        this.endTick = startTick + Util.seconds2GameTick(DURATION_SECONDS);
        this.nextDamageTick = this.startTick; // Get damage immediately
        this.active = true;
    }

    @Override
    public void update(CastSkillContext context) {
        if (!this.active) return;

        long currentTick = context.getCurrentTick();

        // Kết thúc skill
        if (currentTick >= endTick) {
            this.active = false;
            return;
        }

        if (currentTick >= nextDamageTick) {
            this.performAOEDamage(context);
            this.nextDamageTick += Util.seconds2GameTick(DAMAGE_INTERVAL_SECONDS);
        }
    }

    private void performAOEDamage(CastSkillContext ctx) {
        CircleShape hitBox = new CircleShape(
            this.getSkillOwner().getCurrentPosition(),
            DAMAGE_RADIUS
        );

        ctx.addExtraData("damage", this.getDamagePerSecond());

        Set<SkillReceivable> hitEntities = SpringContextHolder.getBean(GameStateService.class)
            .getSkillReceivableEnemiesInScope(ctx.getGameState(), hitBox, this.getSkillOwner().getOwnerSlot());

        hitEntities.stream()
            .forEach(entity -> entity.receiveSkillDamage(ctx));


    }
}
