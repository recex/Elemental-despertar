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

/** Raio: Thunderbolt, Discharge, Stormcloud, Lightning Cloak. */
public final class LightningAbilities {
    private static final Element E = Element.LIGHTNING;

    public static final AbilitySet SET = new AbilitySet()
            .addCharge("Thunderbolt", 25, LightningAbilities::thunderbolt)
            .add("Discharge", 140, LightningAbilities::discharge)
            .add("Stormcloud", 200, LightningAbilities::stormcloud)
            .add("Lightning Cloak", 400, LightningAbilities::cloak);

    private LightningAbilities() {}

    private static boolean thunderbolt(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8F, 1.6F);
        Vec3 end = Shots.beam(p, 40, 0.8, ParticleTypes.ELECTRIC_SPARK, e -> {
            AbilityUtil.hit(p, e, 7.0F);
            AbilityUtil.lightning(level, e.position());
        });
        AbilityUtil.fx(level, ParticleTypes.ELECTRIC_SPARK, end, 25, 0.5, 0.2);
        if (Charge.full(p)) { // MAX CAST: o raio vira um orbe que deixa uma zona de relampagos
            AbilityUtil.circle(level, end, E, 5.5F, 90);
            Scheduler.repeat(8, 10, i -> {
                Vec3 q = AbilityUtil.randomInDisc(p.getRandom(), end, 4.0);
                AbilityUtil.lightning(level, q);
                AbilityUtil.blast(p, q, 1.8, 4.0F, 20, 0.2, 0.3);
            });
        }
        return true;
    }

    private static boolean discharge(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 7.4F, 30);
        Scheduler.repeat(12, 1, i -> AbilityUtil.fx(level, ParticleTypes.ELECTRIC_SPARK, c.add(0, 1, 0), 15, 0.5 + i * 0.4, 0.3));
        Scheduler.run(12, () -> {
            AbilityUtil.sound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5F, 1.0F);
            AbilityUtil.blast(p, c, 7, 6.0F, 50, 0.3, 0.2);
            AbilityUtil.ring(level, c, 7, ParticleTypes.ELECTRIC_SPARK);
            for (int k = 0; k < 5; k++) AbilityUtil.lightning(level, AbilityUtil.randomInDisc(p.getRandom(), c, 7));
        });
        return true;
    }

    private static boolean stormcloud(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        Vec3 cloud = c.add(0, 5, 0);
        AbilityUtil.circle(level, c, E, 5.4F, 170);
        Scheduler.repeat(8, 20, i -> {
            AbilityUtil.fx(level, ParticleTypes.CLOUD, cloud, 60, 2.5, 0.01);
            List<LivingEntity> list = AbilityUtil.around(p, c, 5, 8);
            if (!list.isEmpty()) {
                LivingEntity t = list.get(p.getRandom().nextInt(list.size()));
                AbilityUtil.lightning(level, t.position());
                AbilityUtil.hit(p, t, 5.0F);
                AbilityUtil.stun(t, 20);
            }
        });
        return true;
    }

    private static boolean cloak(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.circle(level, p.position(), E, 2.6F, 200);
        Vec3 end = AbilityUtil.rayEnd(p, 9);
        AbilityUtil.line(level, ParticleTypes.ELECTRIC_SPARK, p.position().add(0, 1, 0), end, 20);
        AbilityUtil.teleport(p, end.subtract(0, p.getEyeHeight(), 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
        Scheduler.repeat(40, 5, i -> {
            if (p.isAlive()) AbilityUtil.fx(level, ParticleTypes.ELECTRIC_SPARK, p.position().add(0, 1, 0), 12, 0.5, 0.1);
        });
        return true;
    }
}
