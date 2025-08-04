package com.server.game.model.game.component.skillComponent;



import org.springframework.stereotype.Component;

import com.server.game.model.game.Champion;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.Util;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Component
@EqualsAndHashCode(exclude = "skillOwner")
public abstract class SkillComponent {
    protected Champion skillOwner;
    protected String name;
    protected float cooldownSeconds;
    protected long cooldownTick;
    protected long lastUsedTick;

    protected CastSkillContext castSkillContext;

    public SkillComponent(Champion owner, ChampionAbility ability) {
        this.skillOwner = owner;
        this.name = ability.getName();
        this.cooldownSeconds = ability.getCooldown();
        
        this.cooldownTick = Util.seconds2GameTick(cooldownSeconds);
        this.lastUsedTick = -cooldownTick; // Initialize to allow immediate use
    }

    public final void setCooldown(float cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
        this.cooldownTick = Util.seconds2GameTick(cooldownSeconds);
    }

    public final float getCooldown() {
        return cooldownSeconds;
    }

    public final long getCooldownTickRemain(long currentTick) {
        long ticksLeft = cooldownTick - (currentTick - this.lastUsedTick);
        return Math.max(ticksLeft, 0);
    }

    public final float getCooldownSecondsRemain(long currentTick) {
        long remainingTicks = this.getCooldownTickRemain(currentTick);
        return remainingTicks * Util.getGameTickIntervalMs() / 1000.0f;
    }

    public final void setCastSkillContext(CastSkillContext ctx) {
        if (castSkillContext != null) {
            System.out.println(">>> [Log in SkillComponent] Skill is using, cannot overwrite another context");
            return;
        }
        this.castSkillContext = ctx;
    }

    public final boolean isReady(long currentTick) {
        return currentTick - this.lastUsedTick >= this.cooldownTick;
    }

    // Template method pattern
    // Wrapper method to ensure cooldown is checked before using the skill
    // Concrete subclasses must only implement the doUse method below
    public final void use(CastSkillContext ctx) {
        long currentTick = ctx.getCurrentTick();
        if (!isReady(currentTick)) return;

        // 1. Broadcast skill usage to the game state
        ctx.getGameStateService().sendCastSkillAnimation(ctx);

        doUse();

        lastUsedTick = currentTick;
    }

    // Protected access modifier to allow subclasses to implement their specific skill logic
    // but not to be called directly
    protected abstract void doUse();
    public abstract void update(); // Nếu có skill cần xử lý theo thời gian

}