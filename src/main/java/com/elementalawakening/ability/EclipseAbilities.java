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

/** Eclipse: Lightfall, Consecrate, Lightblast, Eclipse + especial Ritual of Balance (R). */
public final class EclipseAbilities {
    private static final Element E = Element.ECLIPSE;

    public static final AbilitySet SET = new AbilitySet()
            .add("Lightfall", 80, EclipseAbilities::lightfall)
            .add("Consecrate", 200, EclipseAbilities::consecrate)
            .add("Lightblast", 100, EclipseAbilities::lightblast)
            .add("Eclipse", 600, EclipseAbilities::eclipse)
            .special("Ritual of Balance", 400, EclipseAbilities::ritual);

    private EclipseAbilities() {}

    private static boolean lightfall(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 4.4F, 60);
        Scheduler.repeat(8, 5, i -> {
            Vec3 q = AbilityUtil.randomInDisc(p.getRandom(), c, 4);
            AbilityUtil.line(level, ParticleTypes.END_ROD, q.add(0, 10, 0), q, 14);
            AbilityUtil.sound(level, q, SoundEvents.BEACON_ACTIVATE, 0.5F, 2.0F);
            AbilityUtil.blast(p, q, 1.6, 3.5F, 0, 0.2, 0);
        });
        return true;
    }

    private static boolean consecrate(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 5.4F, 150);
        Scheduler.repeat(14, 10, i -> {
            AbilityUtil.ring(level, c, 5, ParticleTypes.END_ROD);
            AbilityUtil.fx(level, ParticleTypes.END_ROD, c.add(0, 0.5, 0), 20, 2.5, 0.05);
            AbilityUtil.blast(p, c, 5, 2.0F, 0, 0, 0);
        });
        return true;
    }

    private static boolean lightblast(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.5F);
        Vec3 end = Shots.beam(p, 35, 0.7, ParticleTypes.END_ROD, e -> AbilityUtil.hit(p, e, 7.0F));
        AbilityUtil.fx(level, ParticleTypes.EXPLOSION, end, 3, 0.5, 0);
        AbilityUtil.circle(level, end, E, 2.9F, 30);
        AbilityUtil.blast(p, end, 2.5, 4.0F, 0, 0.4, 0.4);
        return true;
    }

    private static boolean eclipse(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 10.5F, 160);
        AbilityUtil.actionbar(p, "Eclipse: carregando (7s)...");
        Scheduler.repeat(7, 20, i -> AbilityUtil.ring(level, c, 2.0 + i * 1.3, ParticleTypes.END_ROD));
        Scheduler.run(140, () -> {
            AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 2.5F, 0.5F);
            AbilityUtil.fx(level, ParticleTypes.EXPLOSION_EMITTER, c.add(0, 1, 0), 1, 0, 0);
            for (double r = 2; r <= 10; r += 2) AbilityUtil.ring(level, c, r, ParticleTypes.FLASH);
            AbilityUtil.blast(p, c, 10, 14.0F, 50, 0.8, 1.2);
        });
        return true;
    }

    /** Sacrifica "energia magica" (aqui: 8 de fome) para recuperar 20% da vida maxima. */
    private static boolean ritual(ServerPlayer p) {
        int food = p.getFoodData().getFoodLevel();
        if (!p.isCreative() && food < 8) {
            AbilityUtil.actionbar(p, "Energia insuficiente (precisa de fome).");
            return false;
        }
        if (!p.isCreative()) p.getFoodData().setFoodLevel(food - 8);
        p.heal(p.getMaxHealth() * 0.2F);
        AbilityUtil.circle(p.serverLevel(), p.position(), E, 2.8F, 40);
        AbilityUtil.fx(p.serverLevel(), ParticleTypes.HEART, p.position().add(0, 1.5, 0), 10, 0.5, 0.1);
        return true;
    }
}
