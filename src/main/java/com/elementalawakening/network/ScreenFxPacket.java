package com.elementalawakening.network;

import com.elementalawakening.fx.ClientFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Efeito de tela no cliente: tremor, clarao, zoom ou barras de cinema. */
public class ScreenFxPacket {
    private final int kind;
    private final int ticks;
    private final float strength;
    private final int color;

    public ScreenFxPacket(int kind, int ticks, float strength, int color) {
        this.kind = kind;
        this.ticks = ticks;
        this.strength = strength;
        this.color = color;
    }

    public static void encode(ScreenFxPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.kind);
        buf.writeVarInt(m.ticks);
        buf.writeFloat(m.strength);
        buf.writeInt(m.color);
    }

    public static ScreenFxPacket decode(FriendlyByteBuf buf) {
        return new ScreenFxPacket(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readInt());
    }

    public static void handle(ScreenFxPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.apply(m.kind, m.ticks, m.strength, m.color)));
        ctx.get().setPacketHandled(true);
    }
}
