package com.elementalawakening.ability;

import net.minecraft.world.entity.LivingEntity;

/** Efeitos temporarios por nome (guardados como "ate quando" no NBT da entidade). */
public final class Buffs {
    private Buffs() {}

    public static void set(LivingEntity e, String key, int ticks) {
        e.getPersistentData().putLong("ea_b_" + key, e.level().getGameTime() + ticks);
    }

    public static boolean active(LivingEntity e, String key) {
        return e.getPersistentData().getLong("ea_b_" + key) > e.level().getGameTime();
    }

    public static void clear(LivingEntity e, String key) {
        e.getPersistentData().remove("ea_b_" + key);
    }
}
