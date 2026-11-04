package com.elementalawakening.network;

import com.elementalawakening.ability.Abilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> Servidor: "usei a habilidade N". Toda a logica roda no servidor. */
public class CastAbilityPacket {
    private final int slot;
    private final int phase; // 0 = apertou a tecla, 1 = soltou

    public CastAbilityPacket(int slot, int phase) {
        this.slot = slot;
        this.phase = phase;
    }

    public static void encode(CastAbilityPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.slot);
        buf.writeByte(msg.phase);
    }

    public static CastAbilityPacket decode(FriendlyByteBuf buf) {
        return new CastAbilityPacket(buf.readByte(), buf.readByte());
    }

    public static void handle(CastAbilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (msg.slot < 0 || msg.slot > 7) return;
            if (msg.phase == 0) Abilities.onPress(player, msg.slot);
            else Abilities.onRelease(player, msg.slot);
        });
        ctx.get().setPacketHandled(true);
    }
}
