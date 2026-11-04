package com.elementalawakening.ability;

import com.elementalawakening.element.Element;
import com.elementalawakening.element.ElementStorage;
import com.elementalawakening.network.ModNetwork;
import com.elementalawakening.network.PlayerStatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/** Manda ao cliente o elemento e as recargas atuais (o servidor continua sendo quem decide tudo). */
public final class StateSync {
    private StateSync() {}

    public static void send(ServerPlayer p) {
        try {
            Element el = ElementStorage.get(p);
            int n = PlayerStatePacket.SLOTS;
            int[] rem = new int[n];
            int[] tot = new int[n];
            boolean[] ch = new boolean[n];
            if (el != null) {
                for (int i = 0; i < n; i++) {
                    AbilitySet.Entry e = Abilities.entryOf(el, i);
                    if (e != null) {
                        tot[i] = e.cooldown();
                        rem[i] = (int) Cooldowns.remaining(p, i);
                        ch[i] = e.charge();
                    }
                }
            }
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    new PlayerStatePacket(el == null ? -1 : el.ordinal(), rem, tot, ch));
        } catch (Exception ignored) {
            // jogador ainda sem conexao (ex.: durante o respawn): o proximo envio corrige
        }
    }
}
