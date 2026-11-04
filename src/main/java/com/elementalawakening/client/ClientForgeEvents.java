package com.elementalawakening.client;

import com.elementalawakening.ElementalMod;
import com.elementalawakening.network.CastAbilityPacket;
import com.elementalawakening.network.ModNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientForgeEvents {
    private static final boolean[] DOWN = new boolean[ClientModEvents.ABILITY_KEYS.length];
    /** Ticks que cada tecla esta sendo segurada (usado pela barra de carga do HUD). */
    public static final int[] HOLD = new int[ClientModEvents.ABILITY_KEYS.length];

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        boolean blocked = mc.player == null || mc.screen != null;

        KeyMapping[] keys = ClientModEvents.ABILITY_KEYS;
        for (int i = 0; i < keys.length; i++) {
            boolean now = !blocked && keys[i].isDown();
            if (now && !DOWN[i]) {
                ModNetwork.CHANNEL.sendToServer(new CastAbilityPacket(i, 0)); // apertou
            } else if (!now && DOWN[i] && mc.player != null) {
                ModNetwork.CHANNEL.sendToServer(new CastAbilityPacket(i, 1)); // soltou
            }
            HOLD[i] = now ? HOLD[i] + 1 : 0;
            DOWN[i] = now;
            while (keys[i].consumeClick()) {
                // limpa cliques acumulados (o controle agora e por aperto/soltura)
            }
        }
    }
}
