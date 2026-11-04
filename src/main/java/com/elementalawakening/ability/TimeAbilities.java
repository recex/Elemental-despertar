package com.elementalawakening.ability;

import com.elementalawakening.element.Element;
import com.elementalawakening.entity.FxEntity;
import com.elementalawakening.entity.MagicCircleEntity;
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

/**
 * Tempo: Revert, Temporal Shift, Time Bomb, Interdiction, Fast Forward, Timeline Destruction
 * + especial Timeline Divergence (R, cria a "timeline": posicao e vida guardadas por 30s).
 */
public final class TimeAbilities {
    private static final Element E = Element.TIME;

    private record Timeline(Vec3 pos, float health, long expires) {}

    private static final Map<UUID, Timeline> LINES = new HashMap<>();

    public static final AbilitySet SET = new AbilitySet()
            .add("Revert", 160, TimeAbilities::revert)
            .add("Temporal Shift", 200, TimeAbilities::temporalShift)
            .add("Time Bomb", 200, TimeAbilities::timeBomb)
            .add("Interdiction", 400, TimeAbilities::interdiction)
            .add("Fast Forward", 400, TimeAbilities::fastForward)
            .add("Timeline Destruction", 800, TimeAbilities::destruction)
            .special("Timeline Divergence", 200, TimeAbilities::divergence);

    private TimeAbilities() {}

    private static Timeline active(ServerPlayer p) {
        Timeline t = LINES.get(p.getUUID());
        if (t != null && t.expires() <= p.level().getGameTime()) {
            LINES.remove(p.getUUID());
            return null;
        }
        return t;
    }

    private static boolean divergence(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        LINES.put(p.getUUID(), new Timeline(p.position(), p.getHealth(), level.getGameTime() + 600));
        AbilityUtil.circle(level, p.position(), E, 2.6F, 200);
        AbilityUtil.fx(level, ParticleTypes.ENCHANT, p.position().add(0, 1, 0), 40, 0.6, 0.5);
        AbilityUtil.actionbar(p, "Timeline criada (30s). Use Revert para voltar.");
        return true;
    }

    private static boolean revert(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Timeline t = active(p);
        if (t != null) {
            LINES.remove(p.getUUID());
            AbilityUtil.circle(level, p.position(), E, 3.0F, 30);
            AbilityUtil.teleport(p, t.pos());
            p.setHealth(Math.min(p.getMaxHealth(), t.health()));
            AbilityUtil.fx(level, ParticleTypes.ENCHANT, p.position().add(0, 1, 0), 50, 0.8, 0.5);
            AbilityUtil.actionbar(p, "Timeline revertida.");
        } else {
            Vec3 c = p.position();
            AbilityUtil.circle(level, c, E, 5.4F, 30);
            AbilityUtil.ring(level, c, 5, ParticleTypes.ENCHANT);
            Scheduler.run(15, () -> {
                AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.2F, 1.0F);
                AbilityUtil.blast(p, c, 5, 7.0F, 0, 0.6, 0.8);
            });
        }
        return true;
    }

    private static boolean temporalShift(ServerPlayer p) {
        Buffs.set(p, "temporal_shift", 100); // se for atacado: teleporta ate o inimigo e acerta com raio (CombatEvents)
        AbilityUtil.circle(p.serverLevel(), p.position(), E, 2.6F, 100);
        AbilityUtil.actionbar(p, "Temporal Shift ativo (5s).");
        return true;
    }

    /**
     * Time Bomb: o projetil e um aro de relogio girando. No impacto abrem-se dois relogios gigantes em pe sobre a zona,
     * o simbolo do Tempo no chao e um "tic-tac": a cada segundo um anel de tempo se expande e o som sobe de tom.
     * No fim a zona estoura num raio.
     */
    private static boolean timeBomb(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.6F);
        Shots.shoot(p, 0.8, 50, ParticleTypes.ENCHANT, (pos, target) -> {
            Vec3 g = AbilityUtil.ground(level, pos);
            AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.8F);
            AbilityUtil.circle(level, g, E, 4.6F, 130);

            float yaw = level.getRandom().nextInt(360);
            Vec3 cc = g.add(0, 2.6, 0);
            MagicCircleEntity.spawnVertical(level, cc, E, 0xFFFFFF, 2.6F, 130, yaw);
            MagicCircleEntity.spawnVertical(level, cc, E, 0xFFFFFF, 2.6F, 130, yaw + 90.0F);

            Scheduler.repeat(6, 20, i -> {
                AbilityUtil.sound(level, g, SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.6F + i * 0.2F);
                Fx.make(level, FxEntity.RING, g.add(0, 0.15, 0), new Vec3(0, 1, 0), 0xFFFFFF, 4.0F, 0.08F, 14, FxEntity.EXPAND);
                AbilityUtil.fx(level, ParticleTypes.ENCHANT, g.add(0, 1.5, 0), 40, 1.8, 0.2);
            });
            Scheduler.repeat(12, 10, i -> {
                AbilityUtil.ring(level, g, 4, ParticleTypes.ENCHANT);
                for (LivingEntity e : AbilityUtil.around(p, g, 4, 3)) {
                    AbilityUtil.hit(p, e, 2.0F);
                    if (e instanceof ServerPlayer sp && LINES.remove(sp.getUUID()) != null) {
                        AbilityUtil.lightning(level, sp.position());
                        AbilityUtil.hit(p, sp, 6.0F);
                    }
                }
            });
            Scheduler.run(126, () -> {
                AbilityUtil.lightning(level, g);
                AbilityUtil.sound(level, g, SoundEvents.GENERIC_EXPLODE, 1.5F, 0.7F);
                AbilityUtil.blast(p, g, 5, 8.0F, 20, 0.6, 1.0);
            });
        });
        return true;
    }

    /**
     * Interdiction (como no jogo): circulo branco no chao; depois de um breve atraso sobe um pilar de luz
     * branca com um bulbo brilhante no topo, e os inimigos na area congelam.
     */
    private static boolean interdiction(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        Vec3 g = AbilityUtil.ground(level, c);
        final int white = 0xFFFFFF;

        MagicCircleEntity.spawn(level, g, E, white, 8.4F, 140);
        AbilityUtil.sound(level, c, SoundEvents.BEACON_ACTIVATE, 1.5F, 0.5F);

        Scheduler.run(10, () -> {
            AbilityUtil.sound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5F, 0.6F);
            Fx.make(level, FxEntity.PILLAR, g, null, white, 1.3F, 16.0F, 110, FxEntity.FLICKER);
            Fx.make(level, FxEntity.PILLAR, g, null, white, 0.6F, 18.0F, 110, 0);
            Fx.make(level, FxEntity.SPHERE, g.add(0, 15.5, 0), null, white, 2.2F, 0.0F, 110, FxEntity.PULSE);
            Fx.make(level, FxEntity.DOME, g, null, white, 3.0F, 0.0F, 110, FxEntity.PULSE);
            Fx.shockwave(level, g, white, 8.0F);

            if (Charge.full(p)) { // MAX CAST: abre uma fenda temporal acima, com explosao enorme
                Vec3 rift = g.add(0, 9, 0);
                MagicCircleEntity.spawnVertical(level, rift, E, white, 6.0F, 60, p.getYRot());
                Fx.make(level, FxEntity.SPHERE, rift, null, white, 2.0F, 0.0F, 60, FxEntity.PULSE);
                Scheduler.run(40, () -> {
                    AbilityUtil.sound(level, g, SoundEvents.GENERIC_EXPLODE, 2.5F, 0.4F);
                    Fx.shockwave(level, g, white, 12.0F);
                    AbilityUtil.blast(p, g, 12, 14.0F, 40, 1.0, 1.6);
                });
            }
            for (LivingEntity e : AbilityUtil.around(p, c, 8, 4)) {
                AbilityUtil.stun(e, 120);
                Buffs.set(e, "interdiction", 120);
                e.getPersistentData().putFloat("ea_time_dmg", 0F);
                AbilityUtil.fx(level, ParticleTypes.SNOWFLAKE, e.position().add(0, 1, 0), 40, 0.5, 0.05);
                Scheduler.run(120, () -> {
                    // dano guardado durante o congelamento volta multiplicado por 1.5
                    Buffs.clear(e, "interdiction");
                    float stored = e.getPersistentData().getFloat("ea_time_dmg");
                    e.getPersistentData().putFloat("ea_time_dmg", 0F);
                    if (stored > 0 && e.isAlive()) AbilityUtil.hit(p, e, stored * 1.5F);
                });
            }
        });
        return true;
    }

    private static boolean fastForward(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
        AbilityUtil.circle(level, p.position(), E, 2.6F, 200);
        Scheduler.repeat(20, 10, i -> {
            if (p.isAlive()) AbilityUtil.fx(level, ParticleTypes.ENCHANT, p.position().add(0, 1, 0), 10, 0.4, 0.05);
        });
        AbilityUtil.actionbar(p, "Fast Forward: velocidade 1.75x por 10s.");
        return true;
    }

    private static boolean destruction(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 12.5F, 60);
        AbilityUtil.sound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.5F);
        for (ServerPlayer sp : new ArrayList<>(level.players())) {
            if (sp == p || sp.distanceToSqr(p) > 25 * 25) continue;
            Timeline t = LINES.remove(sp.getUUID());
            if (t == null) continue;
            AbilityUtil.teleport(sp, t.pos());
            AbilityUtil.lightning(level, t.pos());
            AbilityUtil.hit(p, sp, 8.0F);
        }
        Scheduler.repeat(6, 8, i -> {
            Vec3 portal = AbilityUtil.randomInDisc(p.getRandom(), c, 11);
            AbilityUtil.fx(level, ParticleTypes.PORTAL, portal.add(0, 2, 0), 40, 0.7, 0.3);
            AbilityUtil.lightning(level, portal);
            AbilityUtil.blast(p, portal, 2.5, 5.0F, 0, 0.3, 0.3);
        });
        return true;
    }
}
