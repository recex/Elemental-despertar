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

/** Escuridao (marcas): Expulsion, Chains of Shadow, Cull, Soul Harvest + especial Imbue (R, marca inimigos). */
public final class DarknessAbilities {
    private static final Element E = Element.DARKNESS;

    public static final AbilitySet SET = new AbilitySet()
            .add("Expulsion", 30, DarknessAbilities::expulsion)
            .add("Chains of Shadow", 160, DarknessAbilities::chains)
            .add("Cull", 120, DarknessAbilities::cull)
            .add("Soul Harvest", 300, DarknessAbilities::soulHarvest)
            .special("Imbue", 100, DarknessAbilities::imbue);

    private DarknessAbilities() {}

    /** Acerta; se o alvo estiver marcado, quebra a marca e causa dano extra. */
    private static void markedHit(ServerPlayer p, LivingEntity e, float dmg) {
        if (Buffs.active(e, "mark")) {
            dmg *= 1.6F;
            Buffs.clear(e, "mark");
            e.removeEffect(MobEffects.GLOWING);
            AbilityUtil.fx(p.serverLevel(), ParticleTypes.SOUL, e.position().add(0, 1, 0), 20, 0.4, 0.1);
        }
        AbilityUtil.hit(p, e, dmg);
    }

    private static boolean expulsion(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AbilityUtil.sound(level, p.position(), SoundEvents.WITHER_SHOOT, 0.6F, 1.5F);
        Shots.shoot(p, 1.4, 28, ParticleTypes.SQUID_INK, (pos, target) -> {
            AbilityUtil.fx(level, ParticleTypes.SQUID_INK, pos, 40, 1.0, 0.1);
            for (LivingEntity e : AbilityUtil.around(p, pos, 2.3, 3)) markedHit(p, e, 5.0F);
            AbilityUtil.circle(level, pos, E, 2.6F, 30);
        });
        return true;
    }

    private static boolean chains(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 6.4F, 70);
        AbilityUtil.sound(level, c, SoundEvents.WITHER_SHOOT, 1.0F, 0.6F);
        for (LivingEntity e : AbilityUtil.around(p, c, 6, 4)) {
            AbilityUtil.stun(e, 60);
            markedHit(p, e, 2.0F);
            AbilityUtil.line(level, ParticleTypes.SMOKE, c, e.position().add(0, 1, 0), 14);
        }
        return true;
    }

    private static boolean cull(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.lightning(level, c);
        AbilityUtil.circle(level, c, E, 3.4F, 30);
        AbilityUtil.fx(level, ParticleTypes.SQUID_INK, c.add(0, 1, 0), 60, 1.2, 0.1);
        for (LivingEntity e : AbilityUtil.around(p, c, 3, 3)) markedHit(p, e, 6.0F);
        return true;
    }

    private static boolean soulHarvest(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 6.4F, 40);
        List<LivingEntity> list = AbilityUtil.around(p, c, 6, 3);
        for (LivingEntity e : list) {
            markedHit(p, e, 4.0F);
            AbilityUtil.line(level, ParticleTypes.SOUL, e.position().add(0, 1, 0), p.position().add(0, 1, 0), 12);
        }
        p.heal(2.0F * list.size());
        AbilityUtil.ring(level, c, 6, ParticleTypes.SOUL);
        return true;
    }

    private static boolean imbue(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 3.4F, 40);
        for (LivingEntity e : AbilityUtil.around(p, c, 3, 3)) {
            Buffs.set(e, "mark", 200);
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            AbilityUtil.fx(level, ParticleTypes.SOUL_FIRE_FLAME, e.position().add(0, 1, 0), 25, 0.4, 0.05);
        }
        return true;
    }
}
