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

/** Gravidade: Crush, Push, Skyhammer, Rise, Fall, Pressure + especial Flux (R). Passiva Singularity em CombatEvents. */
public final class GravityAbilities {
    private static final Element E = Element.GRAVITY;
    private static final BlockParticleOption DIRT =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());

    private record Debris(Vec3 pos, long expires) {}

    private static final Map<UUID, Debris> DEBRIS = new HashMap<>();

    public static final AbilitySet SET = new AbilitySet()
            .add("Crush", 80, GravityAbilities::crush)
            .add("Push", 40, GravityAbilities::push)
            .add("Skyhammer", 160, GravityAbilities::skyhammer)
            .add("Rise", 200, GravityAbilities::rise)
            .add("Fall", 120, GravityAbilities::fall)
            .add("Pressure", 120, GravityAbilities::pressure)
            .special("Flux", 160, GravityAbilities::flux);

    private GravityAbilities() {}

    private static boolean crush(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 4.0F, 50);
        Scheduler.repeat(4, 10, i -> {
            AbilityUtil.ring(level, c, 3.5 - i * 0.6, ParticleTypes.PORTAL);
            AbilityUtil.fx(level, ParticleTypes.SQUID_INK, c.add(0, 0.5, 0), 20, 1.5, 0.02);
            for (LivingEntity e : AbilityUtil.around(p, c, 3.5, 4)) {
                AbilityUtil.hit(p, e, 4.0F);
                AbilityUtil.stun(e, 20);
                AbilityUtil.launch(e, 0, -0.8, 0);
            }
        });
        return true;
    }

    private static boolean push(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.6F);
        Shots.shoot(p, 1.6, 20, ParticleTypes.PORTAL, (pos, target) -> {
            AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.7F);
            AbilityUtil.fx(level, ParticleTypes.POOF, pos, 40, 1.2, 0.15);
            AbilityUtil.blast(p, pos, 3.2, 6.0F, 0, 0.6, 1.8);
            AbilityUtil.circle(level, pos, E, 3.4F, 30);
        });
        return true;
    }

    private static boolean skyhammer(ServerPlayer p) {
        if (p.onGround() && !p.isCreative()) {
            AbilityUtil.actionbar(p, "Skyhammer precisa ser usado no ar.");
            return false;
        }
        ServerLevel level = p.serverLevel();
        double startY = p.getY();
        boolean[] done = {false};
        Scheduler.repeat(40, 1, i -> {
            if (done[0] || !p.isAlive()) return;
            p.resetFallDistance();
            if (p.onGround() && i > 1) {
                done[0] = true;
                double h = Math.max(0, startY - p.getY());
                double r = Math.min(10.0, 4.0 + h * 0.3);
                Vec3 c = p.position();
                AbilityUtil.circle(level, c, E, (float) (r * 1.1), 40);
                AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 2.0F, 0.5F);
                AbilityUtil.fx(level, DIRT, c.add(0, 0.5, 0), 100, r * 0.5, 0.3);
                AbilityUtil.ring(level, c, r, ParticleTypes.POOF);
                AbilityUtil.blast(p, c, r, (float) (6.0 + h * 0.6), 40, 0.8, 0.8);
            } else {
                p.setDeltaMovement(0, -2.0, 0);
                p.hurtMarked = true;
            }
        });
        return true;
    }

    private static boolean rise(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 4.4F, 60);
        AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.5F);
        AbilityUtil.fx(level, DIRT, c.add(0, 1, 0), 120, 2.0, 0.25);
        AbilityUtil.blast(p, c, 4, 3.0F, 0, 1.3, 0);
        DEBRIS.put(p.getUUID(), new Debris(c, level.getGameTime() + 200)); // para o Fall
        return true;
    }

    private static boolean fall(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Debris d = DEBRIS.get(p.getUUID());
        if (d == null || d.expires() <= level.getGameTime()) {
            AbilityUtil.actionbar(p, "Use Rise primeiro (os destroços somem em 10s).");
            return false;
        }
        DEBRIS.remove(p.getUUID());
        Vec3 target = AbilityUtil.cursor(p, 40);
        for (int i = 0; i < 5; i++) {
            Vec3 start = AbilityUtil.randomInDisc(p.getRandom(), d.pos(), 3).add(0, 1.5, 0);
            Shots.shootFrom(p, start, target.subtract(start), 1.2, 50, DIRT, (pos, t) -> {
                AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 0.6F, 1.2F);
                AbilityUtil.fx(level, DIRT, pos, 30, 0.8, 0.2);
                AbilityUtil.blast(p, pos, 2.4, 5.0F, 0, 0.4, 0.6);
            });
        }
        return true;
    }

    private static boolean pressure(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 7.4F, 40);
        AbilityUtil.ring(level, c, 7, ParticleTypes.SQUID_INK);
        AbilityUtil.fx(level, ParticleTypes.SQUID_INK, c.add(0, 1, 0), 60, 3.0, 0.02);
        for (LivingEntity e : AbilityUtil.around(p, c, 7, 4)) {
            AbilityUtil.hit(p, e, 3.0F);
            AbilityUtil.stun(e, 20);
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        }
        // esmaga projeteis
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                c.x - 7, c.y - 1, c.z - 7, c.x + 7, c.y + 6, c.z + 7);
        for (net.minecraft.world.entity.projectile.Projectile proj :
                level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, box)) {
            proj.discard();
        }
        return true;
    }

    private static boolean flux(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 3.4F, 30);
        AbilityUtil.fx(level, ParticleTypes.PORTAL, c.add(0, 0.5, 0), 60, 1.2, 0.3);
        AbilityUtil.blast(p, c, 3, 3.0F, 0, 1.1, 0);
        AbilityUtil.launch(p, p.getDeltaMovement().x, 1.4, p.getDeltaMovement().z);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
        return true;
    }
}
