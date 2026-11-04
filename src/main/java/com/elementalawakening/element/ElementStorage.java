package com.elementalawakening.element;

import net.minecraft.world.entity.player.Player;

public final class ElementStorage {
    private static final String KEY = "ea_element";

    private ElementStorage() {}

    public static void set(Player player, Element element) {
        player.getPersistentData().putString(KEY, element.name());
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.elementalawakening.ability.StateSync.send(sp);
        }
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(KEY);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.elementalawakening.ability.StateSync.send(sp);
        }
    }

    /** Retorna null se o jogador ainda nao girou. */
    public static Element get(Player player) {
        String s = player.getPersistentData().getString(KEY);
        if (s.isEmpty()) return null;
        try {
            return Element.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
