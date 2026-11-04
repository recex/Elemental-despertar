package com.elementalawakening.client;

import com.elementalawakening.ElementalMod;
import com.elementalawakening.ability.Abilities;
import com.elementalawakening.element.Element;
import com.elementalawakening.network.PlayerStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** HUD: elemento atual, uma barra por habilidade (tecla, nome, recarga) e a barra de carga do Max cast. */
@Mod.EventBusSubscriber(modid = ElementalMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Hud {
    private Hud() {}

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        Element[] all = Element.values();
        if (ClientState.element < 0 || ClientState.element >= all.length) return;

        Element el = all[ClientState.element];
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();
        long now = mc.level.getGameTime();
        int argb = 0xFF000000 | el.color;

        int rows = 0;
        for (int i = 0; i < PlayerStatePacket.SLOTS; i++) {
            if (Abilities.nameOf(el, i) != null) rows++;
        }
        int x = 6;
        int y = h - 10 - rows * 14 - 12;

        g.drawString(mc.font, el.displayName + " · " + el.rarity.displayName, x, y, argb, true);
        y += 12;

        for (int i = 0; i < PlayerStatePacket.SLOTS; i++) {
            String name = Abilities.nameOf(el, i);
            if (name == null) continue;

            long rem = Math.max(0L, ClientState.END[i] - now);
            int total = Math.max(1, ClientState.TOTAL[i]);
            boolean ready = rem <= 0;
            int bw = 124;

            g.fill(x, y, x + bw, y + 12, 0xAA000000);
            float frac = ready ? 1.0F : 1.0F - rem / (float) total;
            int fillAlpha = ready ? 0x99 : 0x55;
            g.fill(x, y, x + (int) (bw * frac), y + 12, (fillAlpha << 24) | el.color);
            g.renderOutline(x, y, bw, 12, ready ? 0xFFFFFFFF : 0xFF555555);

            String key = ClientModEvents.ABILITY_KEYS[i].getTranslatedKeyMessage().getString();
            String label = "[" + key + "] " + name + (ClientState.CHARGE[i] ? " »" : "");
            g.drawString(mc.font, label, x + 3, y + 2, ready ? 0xFFFFFFFF : 0xFFAAAAAA, true);
            if (!ready) {
                g.drawString(mc.font, String.format("%.1f", rem / 20.0), x + bw - 26, y + 2, 0xFFFFDD66, true);
            }
            y += 14;
        }

        // barra de carga do Max cast (segurando uma tecla de golpe "de segurar")
        for (int i = 0; i < PlayerStatePacket.SLOTS; i++) {
            if (!ClientState.CHARGE[i] || ClientForgeEvents.HOLD[i] <= 6) continue;
            float charge = Mth.clamp((ClientForgeEvents.HOLD[i] - 6) / 50.0F, 0.0F, 1.0F);
            int bw = 120;
            int bx = w / 2 - bw / 2;
            int by = h - 62;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + 7, 0xCC000000);
            g.fill(bx, by, bx + (int) (bw * charge), by + 6, argb);
            if (charge >= 1.0F) g.renderOutline(bx - 1, by - 1, bw + 2, 8, 0xFFFFFFFF);
            String t = charge >= 1.0F ? "MAX!" : (int) (charge * 100) + "%";
            g.drawCenteredString(mc.font, t, w / 2, by - 11, 0xFFFFFFFF);
            break;
        }
    }
}
