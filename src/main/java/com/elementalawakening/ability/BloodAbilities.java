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

/** Sangue: Transfusion, Plague, Blood Barrier, Blood Manipulation + especial Blood Sentinels (R). */
public final class BloodAbilities {
    private static final Element E = Element.BLOOD;

    public static final AbilitySet SET = new AbilitySet()
            .add("Transfusion", 80, BloodAbilities::transfusion)
            .add("Plague", 240, BloodAbilities::plague)
            .add("Blood Barrier", 300, BloodAbilities::barrier)
            .add("Blood Manipulation", 200, BloodAbilities::manipulation)
            .special("Blood Sentinels", 400, BloodAbilities::sentinels);

    private BloodAbilities() {}

    private static boolean transfusion(ServerPlayer p) {
        LivingEntity t = AbilityUtil.targetEntity(p, 12);
        if (t == null) {
            AbilityUtil.actionbar(p, "Mire em um alvo.");
            return false;
        }
        ServerLevel level = p.serverLevel();
        AbilityUtil.hit(p, t, 5.0F);
        p.heal(p.getMaxHealth() * 0.1F);
        AbilityUtil.line(level, AbilityUtil.BLOOD_DUST, t.position().add(0, 1, 0), p.position().add(0, 1, 0), 16);
        AbilityUtil.circle(level, t.position(), E, 2.2F, 30);
        return true;
    }

    private static boolean plague(ServerPlayer p) {
        if (!AbilityUtil.payHealth(p, 0.05F)) return false;
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 3.6F, 330);
        Scheduler.repeat(16, 20, i -> {
            AbilityUtil.fx(level, AbilityUtil.BLOOD_DUST, c.add(0, 0.3, 0), 40, 1.6, 0.02);
            for (LivingEntity e : AbilityUtil.around(p, c, 3, 3)) {
                AbilityUtil.hit(p, e, 1.0F);
                AbilityUtil.stun(e, 10);
                Buffs.set(e, "plague", 100); // recebe 1.25x de dano por 5s
            }
        });
        return true;
    }

    private static boolean barrier(ServerPlayer p) {
        if (!AbilityUtil.payHealth(p, 0.10F)) return false;
        ServerLevel level = p.serverLevel();
        Buffs.set(p, "barrier", 80); // 4s sem receber dano
        AbilityUtil.circle(level, p.position(), E, 2.6F, 80);
        Scheduler.repeat(4, 20, i -> {
            if (p.isAlive()) AbilityUtil.ring(level, p.position(), 1.2, AbilityUtil.BLOOD_DUST);
        });
        return true;
    }

    private static boolean manipulation(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 9.6F, 40);
        for (LivingEntity e : AbilityUtil.around(p, c, 9, 4)) {
            AbilityUtil.hit(p, e, Buffs.active(e, "plague") ? 8.0F : 4.0F);
            p.heal(p.getMaxHealth() * 0.05F);
            AbilityUtil.line(level, AbilityUtil.BLOOD_DUST, e.position().add(0, 1, 0), p.position().add(0, 1, 0), 14);
        }
        AbilityUtil.ring(level, c, 9, AbilityUtil.BLOOD_DUST);
        return true;
    }

    private static boolean sentinels(ServerPlayer p) {
        if (!AbilityUtil.payHealth(p, 0.15F)) return false;
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        Vec3 orb = c.add(0, 1.5, 0);
        AbilityUtil.circle(level, c, E, 2.8F, 200);
        Scheduler.repeat(13, 15, i -> {
            AbilityUtil.fx(level, AbilityUtil.BLOOD_DUST, orb, 20, 0.3, 0.02);
            List<LivingEntity> list = AbilityUtil.around(p, c, 8, 6);
            if (!list.isEmpty()) {
                LivingEntity t = list.get(0);
                AbilityUtil.line(level, AbilityUtil.BLOOD_DUST, orb, t.position().add(0, 1, 0), 12);
                AbilityUtil.hit(p, t, 2.5F);
            }
        });
        return true;
    }
}
