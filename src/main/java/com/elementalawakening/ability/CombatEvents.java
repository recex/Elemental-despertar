package com.elementalawakening.ability;

import com.elementalawakening.ElementalMod;
import com.elementalawakening.element.Element;
import com.elementalawakening.element.ElementStorage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Efeitos que mexem no dano: buffs, debuffs e passivas (Airbourne, Singularity...). */
@Mod.EventBusSubscriber(modid = ElementalMod.MODID)
public class CombatEvents {
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        Entity source = event.getSource().getEntity();
        float amount = event.getAmount();

        // Blood Barrier: ignora todo o dano
        if (Buffs.active(victim, "barrier")) {
            event.setCanceled(true);
            return;
        }
        // Interdiction: o dano fica guardado e volta depois (x1.5)
        if (Buffs.active(victim, "interdiction")) {
            CompoundTag tag = victim.getPersistentData();
            tag.putFloat("ea_time_dmg", tag.getFloat("ea_time_dmg") + amount);
            event.setCanceled(true);
            return;
        }

        // bonus de quem ataca
        if (source instanceof ServerPlayer attacker) {
            if (Buffs.active(attacker, "solfire")) amount *= 1.75F;
            if (Buffs.active(attacker, "traveler")) amount *= 1.5F;
            if (!attacker.onGround() && ElementStorage.get(attacker) == Element.WIND) amount *= 1.25F; // Airbourne
        }

        // debuffs / defesas de quem apanha
        if (Buffs.active(victim, "plague")) amount *= 1.25F;
        if (Buffs.active(victim, "neptune")) amount *= 2.0F;
        if (Buffs.active(victim, "stoneskin")) amount *= 0.5F;
        if (Buffs.active(victim, "singularity")) amount *= 0.5F;

        if (victim instanceof ServerPlayer vp) {
            // Singularity (passiva da Gravidade): pequena chance ao sofrer dano
            if (ElementStorage.get(vp) == Element.GRAVITY && !Buffs.active(vp, "singularity")
                    && vp.getRandom().nextFloat() < 0.05F) {
                Buffs.set(vp, "singularity", 160);
                vp.serverLevel().sendParticles(ParticleTypes.PORTAL, vp.getX(), vp.getY() + 1, vp.getZ(),
                        60, 0.6, 0.8, 0.6, 0.4);
                AbilityUtil.actionbar(vp, "Singularity! Defesa 2x por 8s.");
            }
            // Temporal Shift: teleporta ate quem atacou e acerta com raio
            if (Buffs.active(vp, "temporal_shift") && source instanceof LivingEntity attackerEntity
                    && attackerEntity != vp) {
                Buffs.clear(vp, "temporal_shift");
                Vec3 back = attackerEntity.position().subtract(
                        AbilityUtil.flat(attackerEntity.getLookAngle()).scale(1.5));
                vp.teleportTo(back.x, back.y, back.z);
                AbilityUtil.lightning(vp.serverLevel(), attackerEntity.position());
                AbilityUtil.hit(vp, attackerEntity, 8.0F);
            }
        }

        event.setAmount(amount);
    }
}
