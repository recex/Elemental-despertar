package com.elementalawakening.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Carga do Max cast (0..1). O valor e medido no SERVIDOR (tempo que a tecla ficou segurada) e escala o dano e a area.
 * "sync" vale durante a execucao imediata do golpe; o valor por jogador vale por 10s (golpes com efeitos atrasados).
 */
public final class Charge {
    private static float sync = 0.0F;

    private Charge() {}

    public static void begin(ServerPlayer p, float charge) {
        sync = charge;
        CompoundTag tag = p.getPersistentData();
        tag.putFloat("ea_charge", charge);
        tag.putLong("ea_charge_until", p.level().getGameTime() + 200);
    }

    public static void end() {
        sync = 0.0F;
    }

    public static float of(ServerPlayer p) {
        CompoundTag tag = p.getPersistentData();
        if (tag.getLong("ea_charge_until") <= p.level().getGameTime()) return 0.0F;
        return tag.getFloat("ea_charge");
    }

    public static float dmgMult(ServerPlayer p) {
        return 1.0F + 0.8F * of(p);
    }

    public static float areaMult(ServerPlayer p) {
        return 1.0F + 0.4F * of(p);
    }

    /** Multiplicador de tamanho dos circulos criados durante a execucao imediata do golpe. */
    public static float syncAreaMult() {
        return 1.0F + 0.4F * sync;
    }

    /** Carga maxima? (para as variantes "Max cast" especificas de cada golpe). */
    public static boolean full(ServerPlayer p) {
        return of(p) >= 0.9F;
    }
}
