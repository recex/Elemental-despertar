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

/** Metal: Metal Blast, Magnetism, Spike Upheaval, Metal Bombardment + especial Metal Rod (R). */
public final class MetalAbilities {
    private static final Element E = Element.METAL;

    public static final AbilitySet SET = new AbilitySet()
            .add("Metal Blast", 30, MetalAbilities::metalBlast)
            .add("Magnetism", 160, MetalAbilities::magnetism)
            .add("Spike Upheaval", 160, MetalAbilities::spikeUpheaval)
            .add("Metal Bombardment", 300, MetalAbilities::bombardment)
            .special("Metal Rod", 120, MetalAbilities::metalRod);

    private MetalAbilities() {}

    private static boolean metalBlast(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.ANVIL_LAND, 0.6F, 1.6F);
        Shots.shoot(p, 1.3, 30, ParticleTypes.CRIT, (pos, target) -> {
            AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 0.8F, 1.6F);
            AbilityUtil.fx(level, ParticleTypes.CRIT, pos, 50, 1.0, 0.3);
            AbilityUtil.blast(p, pos, 2.5, 5.0F, 0, 0.3, 0.3);
            Vec3 g = AbilityUtil.ground(level, pos);
            AbilityUtil.circle(level, g, E, 3.4F, 130); // campo de espinhos
            Scheduler.repeat(12, 10, i -> {
                AbilityUtil.fx(level, ParticleTypes.CRIT, g.add(0, 0.3, 0), 15, 1.5, 0.05);
                for (LivingEntity e : AbilityUtil.around(p, g, 3, 2)) AbilityUtil.hit(p, e, 2.0F);
            });
        });
        return true;
    }

    private static boolean magnetism(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 5.4F, 25);
        AbilityUtil.sound(level, c, SoundEvents.ANVIL_LAND, 1.0F, 0.6F);
        AbilityUtil.ring(level, c, 5, ParticleTypes.ENCHANTED_HIT);
        AbilityUtil.blast(p, c, 5, 5.0F, 0, 0.8, 1.2);
        return true;
    }

    private static boolean spikeUpheaval(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 2.9F, 40);
        AbilityUtil.sound(level, c, SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
        AbilityUtil.column(level, ParticleTypes.CRIT, c, 6, 8, 0.3);
        AbilityUtil.blast(p, c, 2.5, 6.0F, 0, 1.0, 0);
        return true;
    }

    private static boolean bombardment(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 5.4F, 60);
        Scheduler.repeat(8, 5, i -> {
            Vec3 q = AbilityUtil.randomInDisc(p.getRandom(), c, 5);
            AbilityUtil.line(level, ParticleTypes.CRIT, q.add(0, 12, 0), q, 14);
            AbilityUtil.sound(level, q, SoundEvents.ANVIL_LAND, 0.8F, 0.8F);
            AbilityUtil.fx(level, ParticleTypes.EXPLOSION, q, 2, 0.5, 0);
            AbilityUtil.blast(p, q, 2.2, 5.0F, 0, 0.4, 0.4);
        });
        return true;
    }

    private static boolean metalRod(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 2.6F, 70);
        AbilityUtil.column(level, ParticleTypes.ENCHANTED_HIT, c, 5, 6, 0.1);
        for (LivingEntity e : AbilityUtil.around(p, c, 2, 3)) {
            AbilityUtil.stun(e, 60);
            AbilityUtil.hit(p, e, 3.0F);
        }
        return true;
    }
}
