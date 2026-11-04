package com.elementalawakening.fx;

import com.elementalawakening.ElementalMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/** Efeitos de tela do cliente (tremor de camera, clarao, zoom, barras de cinema). */
@Mod.EventBusSubscriber(modid = ElementalMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientFx {
    public static final int SHAKE = 0, FLASH = 1, ZOOM = 2, BARS = 3;

    private static final Random RNG = new Random();

    private static int shakeT, shakeMax;
    private static float shakeS;
    private static int flashT, flashMax, flashColor;
    private static float flashA;
    private static int zoomT, zoomMax;
    private static float zoomS;
    private static int barsT, barsMax;

    private ClientFx() {}

    public static void apply(int kind, int ticks, float strength, int color) {
        switch (kind) {
            case SHAKE -> {
                if (strength >= shakeS * ((float) shakeT / Math.max(1, shakeMax))) {
                    shakeT = ticks;
                    shakeMax = ticks;
                    shakeS = strength;
                }
            }
            case FLASH -> {
                flashT = ticks;
                flashMax = ticks;
                flashA = Math.min(1.0F, strength);
                flashColor = color & 0xFFFFFF;
            }
            case ZOOM -> {
                zoomT = ticks;
                zoomMax = ticks;
                zoomS = strength;
            }
            case BARS -> {
                barsT = ticks;
                barsMax = ticks;
            }
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (shakeT > 0) shakeT--;
        if (flashT > 0) flashT--;
        if (zoomT > 0) zoomT--;
        if (barsT > 0) barsT--;
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (shakeT <= 0) return;
        float k = (float) shakeT / Math.max(1, shakeMax);
        float amp = shakeS * k * k * 2.2F;
        event.setYaw(event.getYaw() + (RNG.nextFloat() - 0.5F) * amp);
        event.setPitch(event.getPitch() + (RNG.nextFloat() - 0.5F) * amp);
        event.setRoll(event.getRoll() + (RNG.nextFloat() - 0.5F) * amp * 0.8F);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (zoomT <= 0) return;
        float k = (float) zoomT / Math.max(1, zoomMax);
        float bell = (float) Math.sin(k * Math.PI); // sobe e volta
        event.setFOV(event.getFOV() * (1.0 + zoomS * bell));
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        if (flashT > 0) {
            float k = (float) flashT / Math.max(1, flashMax);
            int a = (int) (flashA * k * k * 255.0F);
            if (a > 0) g.fill(0, 0, w, h, (a << 24) | flashColor);
        }
        if (barsT > 0) {
            float open = Math.min(1.0F, Math.min((barsMax - barsT) / 8.0F, barsT / 8.0F));
            int bar = (int) (h * 0.12F * open);
            if (bar > 0) {
                g.fill(0, 0, w, bar, 0xFF000000);
                g.fill(0, h - bar, w, h, 0xFF000000);
            }
        }
    }

    public static boolean active() {
        return Minecraft.getInstance().level != null && (shakeT > 0 || flashT > 0 || zoomT > 0 || barsT > 0);
    }
}
