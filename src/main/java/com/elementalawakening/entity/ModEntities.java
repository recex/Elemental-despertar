package com.elementalawakening.entity;

import com.elementalawakening.ElementalMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ElementalMod.MODID);

    public static final RegistryObject<EntityType<MagicCircleEntity>> MAGIC_CIRCLE =
            ENTITIES.register("magic_circle", () ->
                    EntityType.Builder.<MagicCircleEntity>of(MagicCircleEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(10)
                            .updateInterval(20)
                            .noSave()
                            .fireImmune()
                            .build("magic_circle"));

    /** Efeito 3D (esferas, pilares, aneis...). updateInterval(1) para acompanhar projeteis suavemente. */
    public static final RegistryObject<EntityType<FxEntity>> FX =
            ENTITIES.register("fx", () ->
                    EntityType.Builder.<FxEntity>of(FxEntity::new, MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .noSave()
                            .fireImmune()
                            .build("fx"));

    private ModEntities() {}
}
