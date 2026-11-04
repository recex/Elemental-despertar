package com.elementalawakening.event;

import com.elementalawakening.ElementalMod;
import com.elementalawakening.ability.Abilities;
import com.elementalawakening.ability.StateSync;
import com.elementalawakening.element.Element;
import com.elementalawakening.element.ElementStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalMod.MODID)
public class PlayerEvents {
    // Mantem o elemento depois de morrer
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Element e = ElementStorage.get(event.getOriginal());
        if (e != null) {
            ElementStorage.set(event.getEntity(), e);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) StateSync.send(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) StateSync.send(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Abilities.forget(event.getEntity().getUUID());
    }
}
