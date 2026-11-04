package com.elementalawakening.network;

import com.elementalawakening.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> Cliente: resultado do sorteio, para a animacao parar no elemento certo. */
public class SpinResultPacket {
    private final int elementId;

    public SpinResultPacket(int elementId) {
        this.elementId = elementId;
    }

    public static void encode(SpinResultPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.elementId);
    }

    public static SpinResultPacket decode(FriendlyByteBuf buf) {
        return new SpinResultPacket(buf.readVarInt());
    }

    public static void handle(SpinResultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onSpinResult(msg.elementId)));
        ctx.get().setPacketHandled(true);
    }
}
