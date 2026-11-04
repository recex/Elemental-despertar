package com.elementalawakening.network;

import com.elementalawakening.ElementalMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ElementalMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, RequestSpinPacket.class,
                RequestSpinPacket::encode, RequestSpinPacket::decode, RequestSpinPacket::handle);
        CHANNEL.registerMessage(id++, SpinResultPacket.class,
                SpinResultPacket::encode, SpinResultPacket::decode, SpinResultPacket::handle);
        CHANNEL.registerMessage(id++, CastAbilityPacket.class,
                CastAbilityPacket::encode, CastAbilityPacket::decode, CastAbilityPacket::handle);
        CHANNEL.registerMessage(id++, ScreenFxPacket.class,
                ScreenFxPacket::encode, ScreenFxPacket::decode, ScreenFxPacket::handle);
        CHANNEL.registerMessage(id++, PlayerStatePacket.class,
                PlayerStatePacket::encode, PlayerStatePacket::decode, PlayerStatePacket::handle);
    }
}
