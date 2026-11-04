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

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.SmallFireball;

/** Fogo: Fireball, Eruption, Fire Pulse, Itto Kaso. Cooldowns sao nossos (a wiki nao informa). */
public final class FireAbilities {
    private static final Element ELEMENT = Element.FIRE;

    public static final AbilitySet SET = new AbilitySet()
            .addCharge("Fireball", 30, FireAbilities::fireball)
            .add("Eruption", 120, FireAbilities::eruption)
            .add("Fire Pulse", 200, FireAbilities::firePulse)
            .add("Itto Kaso", 600, FireAbilities::ittoKaso);

    private FireAbilities() {}

    /** Fireball: bola de fogo 3D que voa e explode. MAX CAST (segurar): 4 lasers de fogo convergem no cursor. */
    private static boolean fireball(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        boolean max = Charge.full(p);
        AbilityUtil.sound(level, p.position(), SoundEvents.BLAZE_SHOOT, 1.0F, max ? 0.6F : 1.0F);
        AbilityUtil.circle(level, p.position(), ELEMENT, 1.8F, 14);

        if (!max) {
            Shots.shoot(p, 1.3, 40, ParticleTypes.FLAME, (pos, t) -> {
                AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 0.9F, 1.2F);
                AbilityUtil.blast(p, pos, 2.4, 6.0F, 0, 0.4, 0.5);
                for (LivingEntity e : AbilityUtil.around(p, pos, 2.4, 3)) e.setSecondsOnFire(3);
            });
            return true;
        }

        Vec3 target = AbilityUtil.cursor(p, 40);
        Vec3 look = p.getLookAngle();
        Vec3 f = AbilityUtil.flat(look);
        Vec3 right = new Vec3(-f.z, 0, f.x);
        Vec3 origin = p.getEyePosition().add(look.scale(0.8));
        double[][] offs = {{1.6, 0.9}, {-1.6, 0.9}, {1.6, -0.6}, {-1.6, -0.6}};
        for (int i = 0; i < 4; i++) {
            final Vec3 start = origin.add(right.scale(offs[i][0])).add(0, offs[i][1], 0);
            Scheduler.run(i * 2, () -> Shots.beamFrom(p, start, target.subtract(start), 45, 0.5, ParticleTypes.FLAME, e -> {
                AbilityUtil.hit(p, e, 5.0F);
                e.setSecondsOnFire(4);
            }));
        }
        Scheduler.run(10, () -> {
            AbilityUtil.sound(level, target, SoundEvents.GENERIC_EXPLODE, 1.5F, 0.7F);
            AbilityUtil.blast(p, target, 4.0, 8.0F, 20, 0.6, 0.8);
            AbilityUtil.circle(level, target, ELEMENT, 5.5F, 50);
        });
        return true;
    }

    private static boolean eruption(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.sound(level, c, SoundEvents.BLAZE_SHOOT, 1.5F, 0.6F);
        AbilityUtil.circle(level, c, ELEMENT, 3.5F, 50);

        Scheduler.repeat(8, 5, i -> {
            for (int h = 0; h < 14; h++) {
                double y = c.y + h * 0.5;
                level.sendParticles(ParticleTypes.FLAME, c.x, y, c.z, 8, 0.6, 0.1, 0.6, 0.02);
                if (h % 3 == 0) level.sendParticles(ParticleTypes.LAVA, c.x, y, c.z, 2, 0.5, 0.1, 0.5, 0.0);
            }
            for (LivingEntity e : AbilityUtil.around(p, c, 2.5, 7)) {
                AbilityUtil.hit(p, e, 3.0F);
                e.setSecondsOnFire(4);
                AbilityUtil.launch(e, e.getDeltaMovement().x, 0.4, e.getDeltaMovement().z);
            }
        });
        return true;
    }

    private static boolean firePulse(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        double radius = 6.0;

        AbilityUtil.sound(level, c, SoundEvents.BLAZE_AMBIENT, 2.0F, 0.5F);
        AbilityUtil.ring(level, c, radius, ParticleTypes.FLAME);
        AbilityUtil.circle(level, c, ELEMENT, 6.4F, 32);
        for (LivingEntity e : AbilityUtil.around(p, c, radius, 3)) AbilityUtil.stun(e, 25);

        Scheduler.run(20, () -> {
            AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.2F, 1.2F);
            for (double r = 1.5; r <= radius; r += 1.5) AbilityUtil.ring(level, c, r, ParticleTypes.FLAME);
            AbilityUtil.ring(level, c, radius, ParticleTypes.EXPLOSION);
            for (LivingEntity e : AbilityUtil.around(p, c, radius, 3)) {
                AbilityUtil.hit(p, e, 6.0F);
                e.setSecondsOnFire(3);
                Vec3 dir = e.position().subtract(c).multiply(1, 0, 1);
                dir = dir.lengthSqr() < 1.0E-4 ? Vec3.ZERO : dir.normalize();
                AbilityUtil.launch(e, dir.x * 1.2, 0.6, dir.z * 1.2);
            }
        });
        return true;
    }

    private static boolean ittoKaso(ServerPlayer p) {
        if (!AbilityUtil.payHealth(p, 0.2F)) return false;

        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        double radius = 4.5;

        AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        AbilityUtil.circle(level, c, ELEMENT, 5.2F, 56);
        for (LivingEntity e : AbilityUtil.around(p, c, radius, 14)) AbilityUtil.stun(e, 45);

        Scheduler.repeat(8, 5, i -> {
            for (int h = 0; h < 28; h++) {
                double y = c.y + h * 0.5;
                level.sendParticles(ParticleTypes.FLAME, c.x, y, c.z, 12, radius * 0.45, 0.1, radius * 0.45, 0.03);
                if (h % 4 == 0) level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, y, c.z, 3, radius * 0.4, 0.1, radius * 0.4, 0.01);
            }
            AbilityUtil.ring(level, c, radius, ParticleTypes.LAVA);
            for (LivingEntity e : AbilityUtil.around(p, c, radius, 14)) {
                AbilityUtil.hit(p, e, 4.0F);
                e.setSecondsOnFire(5);
            }
        });
        return true;
    }
}
