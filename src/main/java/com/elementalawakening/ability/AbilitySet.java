package com.elementalawakening.ability;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Lista de habilidades de um elemento.
 * add(...)     -> teclas Z X C V B N (slots 0..5), na ordem em que forem adicionadas
 * special(...) -> tecla R (slot 6)
 * A acao devolve true se a habilidade foi usada (ai o cooldown comeca).
 */
public final class AbilitySet {
    /** charge = true: segure a tecla para carregar (Max cast); false: sai ao apertar. */
    public record Entry(String name, int cooldown, Predicate<ServerPlayer> action, boolean charge) {}

    public static final int SPECIAL = 6;

    private final Map<Integer, Entry> entries = new HashMap<>();
    private int next = 0;

    public AbilitySet add(String name, int cooldownTicks, Predicate<ServerPlayer> action) {
        entries.put(next++, new Entry(name, cooldownTicks, action, cooldownTicks >= 180));
        return this;
    }

    /** Golpe de segurar (Max cast) mesmo com recarga curta. */
    public AbilitySet addCharge(String name, int cooldownTicks, Predicate<ServerPlayer> action) {
        entries.put(next++, new Entry(name, cooldownTicks, action, true));
        return this;
    }

    public AbilitySet special(String name, int cooldownTicks, Predicate<ServerPlayer> action) {
        entries.put(SPECIAL, new Entry(name, cooldownTicks, action, cooldownTicks >= 180));
        return this;
    }

    public Entry get(int slot) {
        return entries.get(slot);
    }
}
