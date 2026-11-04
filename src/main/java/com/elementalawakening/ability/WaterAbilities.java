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

/** Agua: Water Blast, Water Bomb, Geyser, Evaporation. */
public final class WaterAbilities {
    private static final Element E = Element.WATER;

    public static final AbilitySet SET = new AbilitySet()
            .add("Water Blast", 25, WaterAbilities::waterBlast)
            .add("Water Bomb", 80, WaterAbilities::waterBomb)
            .add("Geyser", 120, WaterAbilities::geyser)
            .add("Evaporation", 240, WaterAbilities::evaporation);

    private WaterAbilities() {}

    private static boolean waterBlast(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.GENERIC_SPLASH, 1.0F, 1.2F);
        Shots.shoot(p, 1.3, 30, ParticleTypes.SPLASH, (pos, target) -> {
            AbilityUtil.fx(level, ParticleTypes.SPLASH, pos, 40, 0.8, 0.1);
            AbilityUtil.blast(p, pos, 2.5, 4.0F, 0, 0.3, 0.3);
            for (LivingEntity e : AbilityUtil.around(p, pos, 2.5, 3)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            AbilityUtil.circle(level, pos, E, 2.4F, 30);
        });
        return true;
    }

    private static boolean waterBomb(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.GENERIC_SPLASH, 1.5F, 0.7F);
        Shots.shoot(p, 1.0, 40, ParticleTypes.BUBBLE_POP, (pos, target) -> {
            AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 1.0F, 1.4F);
            AbilityUtil.column(level, ParticleTypes.SPLASH, pos, 7, 10, 0.8);
            AbilityUtil.blast(p, pos, 3.2, 7.0F, 0, 0.7, 0.2);
            AbilityUtil.circle(level, pos, E, 3.4F, 40);
        });
        return true;
    }

    private static boolean geyser(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 3.0F, 40);
        Scheduler.repeat(5, 4, i -> {
            AbilityUtil.sound(level, c, SoundEvents.GENERIC_SPLASH, 1.0F, 0.8F);
            AbilityUtil.column(level, ParticleTypes.SPLASH, c, 7, 12, 0.6);
            AbilityUtil.blast(p, c, 2.0, 3.5F, 0, 0.9, 0);
        });
        return true;
    }

    private static boolean evaporation(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 4.5F, 110);
        Scheduler.repeat(10, 10, i -> {
            AbilityUtil.fx(level, ParticleTypes.CLOUD, c.add(0, 1, 0), 40, 2.0, 0.02);
            for (LivingEntity e : AbilityUtil.around(p, c, 4, 3)) {
                AbilityUtil.hit(p, e, 2.0F);
                AbilityUtil.stun(e, 15);
            }
        });
        return true;
    }
}
