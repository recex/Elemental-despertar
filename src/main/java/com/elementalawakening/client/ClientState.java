package com.elementalawakening.client;

import com.elementalawakening.network.PlayerStatePacket;
import net.minecraft.client.Minecraft;

/** Estado recebido do servidor (so para desenhar o HUD; quem decide tudo e o servidor). */
public final class ClientState {
    public static int element = -1;
    public static final long[] END = new long[PlayerStatePacket.SLOTS]; // tick em que a recarga termina
    public static final int[] TOTAL = new int[PlayerStatePacket.SLOTS];
    public static final boolean[] CHARGE = new boolean[PlayerStatePacket.SLOTS];

    private ClientState() {}

    public static void apply(PlayerStatePacket m) {
        long now = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        element = m.element;
        for (int i = 0; i < PlayerStatePacket.SLOTS; i++) {
            END[i] = now + m.remaining[i];
            TOTAL[i] = m.total[i];
            CHARGE[i] = m.charge[i];
        }
    }
}
