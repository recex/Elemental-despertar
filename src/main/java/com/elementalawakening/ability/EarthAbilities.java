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

/** Terra: passiva Earthbound (so em terreno solido, dano maior) + Tremor, Earth Wall, Stoneskin, Fissure. */
public final class EarthAbilities {
    private static final Element E = Element.EARTH;
    private static final float MULT = 1.3F; // dano acima da media (Earthbound)
    private static final BlockParticleOption DIRT =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());

    public static final AbilitySet SET = new AbilitySet()
            .add("Tremor", 40, p -> grounded(p) && tremor(p))
            .add("Earth Wall", 140, p -> grounded(p) && earthWall(p))
            .add("Stoneskin", 400, p -> grounded(p) && stoneskin(p))
            .add("Fissure", 240, p -> grounded(p) && fissure(p));

    private EarthAbilities() {}

    private static boolean grounded(ServerPlayer p) {
        if (p.onGround() || p.isCreative()) return true;
        AbilityUtil.actionbar(p, "Terra precisa de terreno sólido (Earthbound).");
        return false;
    }

    private static boolean tremor(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = AbilityUtil.cursor(p, 30);
        AbilityUtil.circle(level, c, E, 2.6F, 40);
        AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 0.8F, 0.6F);
        AbilityUtil.column(level, DIRT, c, 4, 14, 0.7);
        AbilityUtil.blast(p, c, 2.2, 5.0F * MULT, 30, 0.8, 0);
        return true;
    }

    private static boolean earthWall(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 fwd = AbilityUtil.flat(p.getLookAngle());
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        Vec3 base = p.position().add(fwd.scale(3));
        for (int w = -2; w <= 2; w++) {
            for (int h = 0; h < 3; h++) {
                Vec3 q = base.add(right.scale(w)).add(0, h, 0);
                AbilityUtil.tempBlock(level, net.minecraft.core.BlockPos.containing(q.x, q.y, q.z),
                        Blocks.STONE.defaultBlockState(), 160);
            }
        }
        AbilityUtil.sound(level, base, SoundEvents.ANVIL_LAND, 1.0F, 0.6F);
        AbilityUtil.fx(level, DIRT, base.add(0, 1, 0), 60, 1.5, 0.1);
        AbilityUtil.circle(level, base, E, 3.0F, 40);
        return true;
    }

    private static boolean stoneskin(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Buffs.set(p, "stoneskin", 600); // 30s, metade do dano recebido
        AbilityUtil.circle(level, p.position(), E, 2.6F, 60);
        Scheduler.repeat(30, 20, i -> {
            if (p.isAlive() && Buffs.active(p, "stoneskin")) {
                AbilityUtil.fx(level, DIRT, p.position().add(0, 1, 0), 10, 0.5, 0.05);
            }
        });
        AbilityUtil.actionbar(p, "Stoneskin: defesa 2x por 30s.");
        return true;
    }

    private static boolean fissure(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        AbilityUtil.circle(level, c, E, 7.4F, 40);
        AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.5F, 0.5F);
        for (double r = 1.5; r <= 7; r += 1.5) AbilityUtil.ring(level, c, r, DIRT);
        AbilityUtil.blast(p, c, 7, 6.0F * MULT, 40, 1.0, 0.3);
        return true;
    }
}
