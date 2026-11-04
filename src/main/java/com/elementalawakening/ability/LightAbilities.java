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

/** Luz: Lightbeam, Beacon, Wrath, Stunning Radiance + especial Blink (R). */
public final class LightAbilities {
    private static final Element E = Element.LIGHT;

    public static final AbilitySet SET = new AbilitySet()
            .add("Lightbeam", 20, LightAbilities::lightbeam)
            .add("Beacon", 160, LightAbilities::beacon)
            .add("Wrath", 160, LightAbilities::wrath)
            .add("Stunning Radiance", 400, LightAbilities::radiance)
            .special("Blink", 200, LightAbilities::blink);

    private LightAbilities() {}

    private static boolean lightbeam(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 0.8F, 2.0F);
        Vec3 end = Shots.beam(p, 40, 0.6, ParticleTypes.END_ROD, e -> AbilityUtil.hit(p, e, 6.0F));
        AbilityUtil.fx(level, ParticleTypes.FLASH, end, 1, 0, 0);
        return true;
    }

    private static boolean beacon(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 7.4F, 30);
        AbilityUtil.sound(level, c, SoundEvents.BEACON_ACTIVATE, 1.5F, 1.5F);
        AbilityUtil.fx(level, ParticleTypes.FLASH, c.add(0, 1, 0), 1, 0, 0);
        AbilityUtil.ring(level, c, 7, ParticleTypes.END_ROD);
        for (LivingEntity e : AbilityUtil.around(p, c, 7, 3)) {
            AbilityUtil.stun(e, 40);
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
        }
        return true;
    }

    private static boolean wrath(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 3.4F, 40);
        Scheduler.repeat(6, 4, i -> {
            AbilityUtil.column(level, ParticleTypes.END_ROD, c, 12, 4, 0.5);
            AbilityUtil.sound(level, c, SoundEvents.BEACON_ACTIVATE, 0.6F, 1.8F);
            AbilityUtil.blast(p, c, 3.0, 3.5F, 0, 0.2, 0);
        });
        return true;
    }

    private static boolean radiance(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 8.4F, 310);
        AbilityUtil.actionbar(p, "Stunning Radiance: 15s");
        Scheduler.repeat(30, 10, i -> {
            for (int k = 0; k < 6; k++) {
                Vec3 q = AbilityUtil.randomInDisc(p.getRandom(), c, 8);
                AbilityUtil.line(level, ParticleTypes.END_ROD, q.add(0, 10, 0), q, 8);
            }
            for (LivingEntity e : AbilityUtil.around(p, c, 8, 6)) AbilityUtil.hit(p, e, 1.2F);
        });
        return true;
    }

    private static boolean blink(ServerPlayer p) {
        Vec3 end = AbilityUtil.rayEnd(p, 30);
        AbilityUtil.circle(p.serverLevel(), p.position(), E, 2.0F, 20);
        AbilityUtil.teleport(p, end.subtract(0, p.getEyeHeight(), 0));
        AbilityUtil.fx(p.serverLevel(), ParticleTypes.END_ROD, p.position().add(0, 1, 0), 40, 0.5, 0.2);
        return true;
    }
}
