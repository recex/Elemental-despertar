package com.elementalawakening.ability;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Cooldowns {
    private static final Map<UUID, long[]> MAP = new HashMap<>();

    private Cooldowns() {}

    /** Ticks que faltam para a habilidade ficar pronta (0 = pronta). */
    public static long remaining(ServerPlayer player, int slot) {
        long[] arr = MAP.get(player.getUUID());
        if (arr == null) return 0;
        return Math.max(0, arr[slot] - player.level().getGameTime());
    }

    public static void clear(UUID id) {
        MAP.remove(id);
    }

    public static void set(ServerPlayer player, int slot, int ticks) {
        long[] arr = MAP.computeIfAbsent(player.getUUID(), k -> new long[8]);
        arr[slot] = player.level().getGameTime() + ticks;
    }
}
