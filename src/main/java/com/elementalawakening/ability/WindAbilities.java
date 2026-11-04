package com.elementalawakening.ability;

import com.elementalawakening.element.Element;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Vento: passiva Airbourne (1.25x no ar, tratada em CombatEvents) + Gale, Rising Wind, Microburst, High Pressure Bomb. */
public final class WindAbilities {
    private static final Element E = Element.WIND;

    public static final AbilitySet SET = new AbilitySet()
            .add("Gale", 25, WindAbilities::gale)
            .add("Rising Wind", 100, WindAbilities::risingWind)
            .add("Microburst", 160, WindAbilities::microburst)
            .add("High Pressure Bomb", 300, WindAbilities::pressureBomb);

    private WindAbilities() {}

    private static boolean gale(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        AbilityUtil.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
        for (int d = 2; d <= 12; d += 2) {
            Vec3 q = p.getEyePosition().add(look.scale(d));
            AbilityUtil.fx(level, ParticleTypes.CLOUD, q, 8, d * 0.18, 0.05);
        }
        for (LivingEntity e : AbilityUtil.cone(p, 12, 0.82)) {
            AbilityUtil.hit(p, e, 3.0F);
            AbilityUtil.launch(e, look.x * 1.6, 0.3, look.z * 1.6);
        }
        return true;
    }

    private static boolean risingWind(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.circle(level, p.position(), E, 2.4F, 25);
        AbilityUtil.fx(level, ParticleTypes.CLOUD, p.position().add(0, 0.3, 0), 40, 0.6, 0.15);
        AbilityUtil.launch(p, p.getDeltaMovement().x * 0.3, 1.7, p.getDeltaMovement().z * 0.3);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false));
        AbilityUtil.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);
        return true;
    }

    private static boolean microburst(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 3.8F, 40);
        AbilityUtil.fx(level, ParticleTypes.CLOUD, c.add(0, 6, 0), 60, 1.2, 0.05);
        Scheduler.run(8, () -> {
            AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.0F, 1.5F);
            AbilityUtil.fx(level, ParticleTypes.POOF, c.add(0, 0.5, 0), 80, 1.8, 0.2);
            AbilityUtil.blast(p, c, 3.5, 8.0F, 30, 0.4, 0.2);
        });
        return true;
    }

    private static boolean pressureBomb(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.actionbar(p, "Acumulando vento...");
        Scheduler.repeat(20, 1, i -> {
            if (p.isAlive()) AbilityUtil.fx(level, ParticleTypes.CLOUD, p.position().add(0, 3, 0), 6 + i, 0.4 + i * 0.03, 0.02);
        });
        Scheduler.run(20, () -> {
            if (!p.isAlive()) return;
            Shots.shootFrom(p, p.position().add(0, 3, 0),
                    AbilityUtil.cursor(p, 50).subtract(p.position().add(0, 3, 0)), 1.0, 60, ParticleTypes.CLOUD,
                    (pos, target) -> {
                        AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 2.0F, 0.8F);
                        AbilityUtil.fx(level, ParticleTypes.POOF, pos, 150, 3.0, 0.3);
                        AbilityUtil.fx(level, ParticleTypes.EXPLOSION_EMITTER, pos, 1, 0, 0);
                        AbilityUtil.blast(p, pos, 6, 14.0F, 40, 1.1, 1.5);
                        AbilityUtil.circle(level, pos, E, 6.4F, 40);
                    });
        });
        return true;
    }
}
