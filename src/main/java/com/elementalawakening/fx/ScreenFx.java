package com.elementalawakening.fx;

import com.elementalawakening.network.ModNetwork;
import com.elementalawakening.network.ScreenFxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Manda efeitos de tela para os jogadores perto de um ponto (a forca cai com a distancia). */
public final class ScreenFx {
    private ScreenFx() {}

    private static void send(ServerLevel level, Vec3 pos, double radius, int kind, int ticks, float strength, int color,
                             boolean fade) {
        for (ServerPlayer sp : level.players()) {
            double d = sp.position().distanceTo(pos);
            if (d > radius) continue;
            float k = fade ? (float) (1.0 - d / radius) : 1.0F;
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new ScreenFxPacket(kind, ticks, strength * k, color));
        }
    }

    public static void shake(ServerLevel level, Vec3 pos, double radius, int ticks, float strength) {
        send(level, pos, radius, ClientFx.SHAKE, ticks, strength, 0, true);
    }

    public static void flash(ServerLevel level, Vec3 pos, double radius, int ticks, float alpha, int rgb) {
        send(level, pos, radius, ClientFx.FLASH, ticks, alpha, rgb, true);
    }

    /** strength > 0 abre o campo de visao (afasta), < 0 fecha (aproxima). */
    public static void zoom(ServerLevel level, Vec3 pos, double radius, int ticks, float strength) {
        send(level, pos, radius, ClientFx.ZOOM, ticks, strength, 0, true);
    }

    public static void bars(ServerLevel level, Vec3 pos, double radius, int ticks) {
        send(level, pos, radius, ClientFx.BARS, ticks, 1.0F, 0, false);
    }
}
