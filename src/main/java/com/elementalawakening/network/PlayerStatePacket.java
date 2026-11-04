package com.elementalawakening.network;

import com.elementalawakening.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: elemento atual e recarga de cada tecla (para o HUD). */
public class PlayerStatePacket {
    public static final int SLOTS = 7;

    public final int element;        // -1 = sem elemento
    public final int[] remaining;    // ticks que faltam
    public final int[] total;        // recarga total em ticks (0 = sem habilidade nessa tecla)
    public final boolean[] charge;   // golpe de segurar?

    public PlayerStatePacket(int element, int[] remaining, int[] total, boolean[] charge) {
        this.element = element;
        this.remaining = remaining;
        this.total = total;
        this.charge = charge;
    }

    public static void encode(PlayerStatePacket m, FriendlyByteBuf buf) {
        buf.writeInt(m.element);
        for (int i = 0; i < SLOTS; i++) {
            buf.writeVarInt(m.remaining[i]);
            buf.writeVarInt(m.total[i]);
            buf.writeBoolean(m.charge[i]);
        }
    }

    public static PlayerStatePacket decode(FriendlyByteBuf buf) {
        int element = buf.readInt();
        int[] rem = new int[SLOTS];
        int[] tot = new int[SLOTS];
        boolean[] ch = new boolean[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            rem[i] = buf.readVarInt();
            tot[i] = buf.readVarInt();
            ch[i] = buf.readBoolean();
        }
        return new PlayerStatePacket(element, rem, tot, ch);
    }

    public static void handle(PlayerStatePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.apply(m)));
        ctx.get().setPacketHandled(true);
    }
}
