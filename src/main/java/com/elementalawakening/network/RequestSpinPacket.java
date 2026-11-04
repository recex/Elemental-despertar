package com.elementalawakening.network;

import com.elementalawakening.ability.AbilityUtil;
import com.elementalawakening.element.Element;
import com.elementalawakening.element.ElementStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Cliente -> Servidor: "quero girar". O servidor sorteia (nunca confie no cliente). */
public class RequestSpinPacket {
    public static void encode(RequestSpinPacket msg, FriendlyByteBuf buf) {}

    public static RequestSpinPacket decode(FriendlyByteBuf buf) {
        return new RequestSpinPacket();
    }

    public static void handle(RequestSpinPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // TODO: exigir/consumir o Orbe, cooldown, etc.
            Element result = Element.roll(player.getRandom());
            ElementStorage.set(player, result);
            AbilityUtil.circle(player.serverLevel(), player.position(), result, 4.0F, 60);

            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SpinResultPacket(result.ordinal())
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
