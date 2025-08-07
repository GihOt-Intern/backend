package com.server.game.model.game.component.skillComponent;
import org.springframework.stereotype.Component;

import com.server.game.model.game.Champion;
import com.server.game.resource.model.ChampionDB.ChampionAbility;
import com.server.game.util.Util;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Component
@Slf4j
@EqualsAndHashCode(callSuper=false)
public abstract class DurationSkillComponent extends SkillComponent {

    protected final float DURATION_SECONDS;
    protected final float DAMAGE_INTERVAL_SECONDS;


    protected long startTick = -1;                // Khi bắt đầu kích hoạt skill
    protected long endTick = -1;                  // Khi kết thúc
    protected long nextDamageTick = -1;           // Tick tiếp theo cần gây damage

    public DurationSkillComponent(Champion owner, ChampionAbility ability,
        float durationSeconds, float damageIntervalSeconds) {
        super(owner, ability);
        DURATION_SECONDS = durationSeconds;
        DAMAGE_INTERVAL_SECONDS = damageIntervalSeconds;
    }


    @Override
    protected final boolean doUse() {
        long currentTick = this.getCastSkillContext().getCurrentTick();

        this.startTick = currentTick;
        this.endTick = startTick + Util.seconds2GameTick(DURATION_SECONDS);
        this.nextDamageTick = this.startTick; // Get damage immediately

        return true;
    }

    public final boolean updatePerTick() {
        if (!this.isActive) { return false; }

        if (this.skillOwner.isMoving() && !this.canUseWhileMoving()) {
            // cast skill is higher priority than moving, so we stop moving
            log.info("Champion {} is moving and received a skill use request, stopping movement.",
                this.skillOwner.getName());

            this.skillOwner.setStopMoving(true);
        }

        if (skillOwner.isAttacking() && !this.canUseWhileAttacking()) {
            // skill has higher priority than attack, so we stop the attack
            log.info("Using skill, but champion is attacking, stopping attack for champion {}.",
                this.skillOwner.getName());

            skillOwner.stopAttacking();
        }

        long currentTick = this.getCastSkillContext().getCurrentTick();

        // Kết thúc skill
        if (currentTick >= endTick) {
            log.info("Skill ended for champion: {}", this.getSkillOwner().getName());
            this.isActive = false; // Mark skill as inactive
            return false;
        }

        if (currentTick < nextDamageTick) {
            // Skill is still active, but no damage this tick
            log.info("Skill: not in damage tick, current tick: {}, nextDamageTick: {}", 
                currentTick, nextDamageTick);
            return true; // Continue to update
        }

        this.performAtCorrectTick();

        this.nextDamageTick += Util.seconds2GameTick(this.DAMAGE_INTERVAL_SECONDS);
        return true;
    }

    protected abstract boolean performAtCorrectTick(); // Nếu có skill cần xử lý theo thời gian
}