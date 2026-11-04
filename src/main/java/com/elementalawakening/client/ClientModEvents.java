package com.elementalawakening.client;

import com.elementalawakening.ElementalMod;
import com.elementalawakening.entity.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ElementalMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {
    private static final String CATEGORY = "key.categories.elementalawakening";

    // slots 0..5 = Z X C V B N, slot 6 = R (habilidade especial)
    public static final KeyMapping[] ABILITY_KEYS = {
            new KeyMapping("key.elementalawakening.ability1", GLFW.GLFW_KEY_Z, CATEGORY),
            new KeyMapping("key.elementalawakening.ability2", GLFW.GLFW_KEY_X, CATEGORY),
            new KeyMapping("key.elementalawakening.ability3", GLFW.GLFW_KEY_C, CATEGORY),
            new KeyMapping("key.elementalawakening.ability4", GLFW.GLFW_KEY_V, CATEGORY),
            new KeyMapping("key.elementalawakening.ability5", GLFW.GLFW_KEY_B, CATEGORY),
            new KeyMapping("key.elementalawakening.ability6", GLFW.GLFW_KEY_N, CATEGORY),
            new KeyMapping("key.elementalawakening.special", GLFW.GLFW_KEY_R, CATEGORY)
    };

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : ABILITY_KEYS) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MAGIC_CIRCLE.get(), MagicCircleRenderer::new);
        event.registerEntityRenderer(ModEntities.FX.get(), FxRenderer::new);
    }
}
