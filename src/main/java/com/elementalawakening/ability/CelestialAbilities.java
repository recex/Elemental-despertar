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

/** Celestial: Solfire, Planet Throw, Lightspeed Lariat, Supernova, Harbinger, Meteor Strike. */
public final class CelestialAbilities {
    private static final Element E = Element.CELESTIAL;

    public static final AbilitySet SET = new AbilitySet()
            .add("Solfire", 220, CelestialAbilities::solfire)
            .add("Planet Throw", 60, CelestialAbilities::planetThrow)
            .add("Lightspeed Lariat", 140, CelestialAbilities::lariat)
            .add("Supernova", 320, CelestialAbilities::supernova)
            .add("Harbinger", 800, CelestialAbilities::harbinger)
            .addCharge("Meteor Strike", 80, CelestialAbilities::meteor);

    private CelestialAbilities() {}

    private static boolean solfire(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Buffs.set(p, "solfire", 200); // dano 1.75x por 10s
        AbilityUtil.circle(level, p.position(), E, 2.8F, 200);
        Scheduler.repeat(20, 10, i -> {
            if (p.isAlive()) AbilityUtil.fx(level, ParticleTypes.FLAME, p.position().add(0, 1, 0), 15, 0.5, 0.03);
        });
        AbilityUtil.actionbar(p, "Solfire: dano 1.75x por 10s.");
        return true;
    }

    private static boolean planetThrow(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int kind = p.getRandom().nextInt(3); // 0 Saturn, 1 Neptune, 2 Black hole
        String[] names = {"Saturn", "Neptune", "Black hole"};
        AbilityUtil.actionbar(p, names[kind]);
        var trail = kind == 0 ? ParticleTypes.END_ROD : (kind == 1 ? ParticleTypes.BUBBLE_POP : ParticleTypes.SQUID_INK);

        Shots.shoot(p, 1.2, 40, trail, (pos, target) -> {
            AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 1.0F, 1.2F);
            if (kind == 0) {
                AbilityUtil.fx(level, ParticleTypes.EXPLOSION, pos, 4, 1.0, 0);
                AbilityUtil.blast(p, pos, 3.0, 7.0F, 0, 0.5, 0.8);
            } else if (kind == 1) {
                if (target != null) {
                    AbilityUtil.hit(p, target, 4.0F);
                    Buffs.set(target, "neptune", 100); // recebe 2x de dano por 5s
                }
                AbilityUtil.fx(level, ParticleTypes.BUBBLE_POP, pos, 40, 1.0, 0.1);
            } else {
                AbilityUtil.fx(level, ParticleTypes.SQUID_INK, pos, 60, 1.5, 0.1);
                Scheduler.repeat(3, 5, i -> {
                    for (LivingEntity e : AbilityUtil.around(p, pos, 5, 4)) AbilityUtil.pull(e, pos, 0.8);
                });
                Scheduler.run(15, () -> AbilityUtil.blast(p, pos, 4.0, 5.0F, 0, 0.4, 0.4));
            }
            AbilityUtil.circle(level, pos, E, 3.4F, 40);
        });
        return true;
    }

    private static boolean lariat(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.circle(level, p.position(), E, 2.4F, 20);
        Scheduler.run(6, () -> {
            if (!p.isAlive()) return;
            Vec3 start = p.position();
            Vec3 end = AbilityUtil.rayEnd(p, 12).subtract(0, p.getEyeHeight(), 0);
            Set<LivingEntity> victims = new HashSet<>();
            for (int i = 0; i <= 12; i++) victims.addAll(AbilityUtil.around(p, start.lerp(end, i / 12.0), 2, 3));
            AbilityUtil.line(level, ParticleTypes.END_ROD, start.add(0, 1, 0), end.add(0, 1, 0), 24);
            AbilityUtil.teleport(p, end);
            Vec3 dir = AbilityUtil.flat(end.subtract(start));
            for (LivingEntity e : victims) {
                AbilityUtil.hit(p, e, 4.0F);
                AbilityUtil.stun(e, 30);
                AbilityUtil.launch(e, dir.x * 1.2, 0.4, dir.z * 1.2);
            }
        });
        return true;
    }

    private static boolean supernova(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 8.4F, 40);
        AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 2.0F, 0.6F);
        AbilityUtil.fx(level, ParticleTypes.EXPLOSION_EMITTER, c.add(0, 1, 0), 1, 0, 0);
        AbilityUtil.fx(level, ParticleTypes.FLASH, c.add(0, 1, 0), 1, 0, 0);
        for (double r = 2; r <= 8; r += 2) AbilityUtil.ring(level, c, r, ParticleTypes.END_ROD);
        AbilityUtil.blast(p, c, 8, 10.0F, 30, 0.7, 1.4);
        return true;
    }

    private static boolean harbinger(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 35);
        Vec3 orb = c.add(0, 2.5, 0);
        AbilityUtil.circle(level, c, E, 7.4F, 110);
        Scheduler.repeat(10, 10, i -> {
            AbilityUtil.fx(level, ParticleTypes.END_ROD, orb, 30, 0.6, 0.05);
            for (LivingEntity e : AbilityUtil.around(p, c, 7, 6)) {
                AbilityUtil.pull(e, orb, 0.6);
                AbilityUtil.hit(p, e, 2.5F);
                AbilityUtil.line(level, ParticleTypes.END_ROD, orb, e.position().add(0, 1, 0), 8);
            }
        });
        return true;
    }

    /** Meteor Strike. MAX CAST (segurar): meteoro gigante, queda mais longa e explosao enorme. */
    private static boolean meteor(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        boolean max = Charge.full(p);
        float rad = max ? 9.0F : 5.0F;
        float dmg = max ? 26.0F : 12.0F;
        int fall = max ? 36 : 24;
        double top = max ? 45.0 : 30.0;
        Vec3 c = AbilityUtil.cursor(p, 40);
        AbilityUtil.circle(level, c, E, rad + 0.4F, fall + 16);

        // corpo 3D do meteoro caindo: rocha em chamas + brilho
        new com.elementalawakening.entity.FxEntity.Spec(com.elementalawakening.entity.FxEntity.ROCK)
                .pos(c.add(0, top, 0)).color(0xFF7A30).r(rad * 0.22F).life(fall).vel(0, -top / fall, 0)
                .style(com.elementalawakening.entity.FxEntity.SOLID | com.elementalawakening.entity.FxEntity.SPIN)
                .spawn(level);
        new com.elementalawakening.entity.FxEntity.Spec(com.elementalawakening.entity.FxEntity.SPHERE)
                .pos(c.add(0, top, 0)).color(0xFFB060).r(rad * 0.32F).life(fall).vel(0, -top / fall, 0)
                .style(com.elementalawakening.entity.FxEntity.FLICKER).spawn(level);

        Scheduler.repeat(fall, 1, i -> {
            double h = top * (1.0 - (double) i / fall);
            AbilityUtil.fx(level, ParticleTypes.FLAME, c.add(0, h, 0), max ? 50 : 25, max ? 1.6 : 0.8, 0.05);
            AbilityUtil.fx(level, ParticleTypes.LARGE_SMOKE, c.add(0, h + 1, 0), 8, 0.6, 0.02);
        });
        Scheduler.run(fall, () -> {
            AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 2.5F, max ? 0.4F : 0.5F);
            AbilityUtil.fx(level, ParticleTypes.EXPLOSION_EMITTER, c.add(0, 1, 0), max ? 4 : 2, rad * 0.2, 0);
            AbilityUtil.blast(p, c, rad, dmg, max ? 30 : 0, 1.0, 1.0);
        });
        return true;
    }
}
