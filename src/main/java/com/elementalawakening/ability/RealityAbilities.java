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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reality Collapse: Sphere of Distortion/Fracture/Collapse, Dissonance, Traveler, The Black Sun + especial Collapse (R). */
public final class RealityAbilities {
    private static final Element E = Element.REALITY_COLLAPSE;

    public static final AbilitySet SET = new AbilitySet()
            .add("Sphere of Distortion", 40, RealityAbilities::distortion)
            .add("Sphere of Fracture", 100, RealityAbilities::fracture)
            .add("Sphere of Collapse", 240, RealityAbilities::sphereCollapse)
            .add("Dissonance", 160, RealityAbilities::dissonance)
            .add("Traveler", 300, RealityAbilities::traveler)
            .add("The Black Sun", 900, RealityAbilities::blackSun)
            .special("Collapse", 160, RealityAbilities::collapse);

    private RealityAbilities() {}

    private static boolean distortion(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.5F);
        Shots.shoot(p, 2.2, 25, ParticleTypes.PORTAL, (pos, target) -> {
            AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 1.5F, 0.7F);
            AbilityUtil.fx(level, ParticleTypes.EXPLOSION_EMITTER, pos, 1, 0, 0);
            AbilityUtil.fx(level, ParticleTypes.REVERSE_PORTAL, pos, 60, 1.5, 0.2);
            AbilityUtil.blast(p, pos, 4, 8.0F, 0, 0.8, 0.8);
            AbilityUtil.circle(level, pos, E, 4.4F, 40);
        });
        return true;
    }

    private static boolean fracture(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        Vec3 sphere = c.add(0, 1.2, 0);
        AbilityUtil.circle(level, c, E, 3.0F, 100);
        Scheduler.repeat(8, 10, i -> {
            AbilityUtil.fx(level, ParticleTypes.REVERSE_PORTAL, sphere, 40, 0.8, 0.2);
            AbilityUtil.fx(level, ParticleTypes.PORTAL, sphere, 20, 0.6, 0.3);
            AbilityUtil.blast(p, c, 2.5, 2.0F, 0, 0.2, 0.2);
        });
        return true;
    }

    /** Envia o alvo "para outro lugar" (atordoado e cego) e ele volta tomando dano pesado. */
    private static boolean sphereCollapse(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Shots.shoot(p, 1.5, 40, ParticleTypes.REVERSE_PORTAL, (pos, target) -> {
            if (target == null) return;
            AbilityUtil.circle(level, target.position(), E, 3.0F, 60);
            AbilityUtil.stun(target, 60);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            AbilityUtil.fx(level, ParticleTypes.PORTAL, target.position().add(0, 1, 0), 60, 0.6, 0.5);
            Scheduler.run(60, () -> {
                if (!target.isAlive()) return;
                AbilityUtil.sound(level, target.position(), SoundEvents.GENERIC_EXPLODE, 1.5F, 0.8F);
                AbilityUtil.fx(level, ParticleTypes.EXPLOSION, target.position().add(0, 1, 0), 5, 0.6, 0);
                AbilityUtil.hit(p, target, 14.0F);
            });
        });
        return true;
    }

    private static boolean dissonance(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 8.4F, 30);
        AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.5F);
        for (double r = 2; r <= 8; r += 2) AbilityUtil.ring(level, c, r, ParticleTypes.REVERSE_PORTAL);
        AbilityUtil.blast(p, c, 8, 5.0F, 30, 1.0, 0.4);
        return true;
    }

    private static boolean traveler(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Buffs.set(p, "traveler", 300); // dano 1.5x por 15s
        p.heal(p.getMaxHealth() * 0.2F);
        AbilityUtil.circle(level, p.position(), E, 2.8F, 300);
        Vec3 end = AbilityUtil.rayEnd(p, 8).subtract(0, p.getEyeHeight(), 0);
        AbilityUtil.teleport(p, end);
        Scheduler.repeat(15, 20, i -> {
            if (p.isAlive()) AbilityUtil.fx(level, ParticleTypes.REVERSE_PORTAL, p.position().add(0, 1, 0), 12, 0.5, 0.1);
        });
        AbilityUtil.actionbar(p, "Traveler: dano 1.5x por 15s.");
        return true;
    }

    private static boolean blackSun(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 35);
        Vec3 core = c.add(0, 3, 0);
        AbilityUtil.circle(level, c, E, 10.5F, 130);
        AbilityUtil.sound(level, c, SoundEvents.ENDERMAN_TELEPORT, 2.0F, 0.4F);
        Scheduler.repeat(12, 10, i -> {
            AbilityUtil.fx(level, ParticleTypes.SQUID_INK, core, 60, 1.0, 0.05);
            AbilityUtil.ring(level, c.add(0, 3, 0), 3.0 + (i % 4), ParticleTypes.REVERSE_PORTAL);
            for (LivingEntity e : AbilityUtil.around(p, c, 10, 8)) {
                AbilityUtil.pull(e, core, 0.6);
                AbilityUtil.hit(p, e, 3.0F);
            }
        });
        Scheduler.run(120, () -> {
            AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 3.0F, 0.4F);
            AbilityUtil.fx(level, ParticleTypes.EXPLOSION_EMITTER, core, 3, 1.5, 0);
            AbilityUtil.blast(p, c, 6, 12.0F, 40, 1.0, 1.0);
        });
        return true;
    }

    private static boolean collapse(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.circle(level, p.position(), E, 2.4F, 20);
        Vec3 end = AbilityUtil.rayEnd(p, 30).subtract(0, p.getEyeHeight(), 0);
        AbilityUtil.teleport(p, end);
        AbilityUtil.circle(level, end, E, 3.6F, 30);
        AbilityUtil.fx(level, ParticleTypes.EXPLOSION, end.add(0, 1, 0), 5, 0.8, 0);
        AbilityUtil.blast(p, end, 3, 6.0F, 0, 0.5, 0.8);
        return true;
    }
}
