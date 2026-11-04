package com.elementalawakening.ability;

import com.elementalawakening.element.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/** Particulas proprias de cada elemento (principal e secundaria) + explosoes e ambiente. */
public final class ElementFx {
    private static final Map<Element, ParticleOptions[]> MAP = new EnumMap<>(Element.class);

    private static void put(Element e, ParticleOptions a, ParticleOptions b) {
        MAP.put(e, new ParticleOptions[]{a, b});
    }

    static {
        put(Element.FIRE, ParticleTypes.FLAME, ParticleTypes.LAVA);
        put(Element.WATER, ParticleTypes.SPLASH, ParticleTypes.BUBBLE_POP);
        put(Element.LIGHTNING, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.CLOUD);
        put(Element.EARTH, ParticleTypes.POOF, ParticleTypes.CRIT);
        put(Element.WIND, ParticleTypes.CLOUD, ParticleTypes.POOF);
        put(Element.LIGHT, ParticleTypes.END_ROD, ParticleTypes.END_ROD);
        put(Element.DARKNESS, ParticleTypes.SQUID_INK, ParticleTypes.SMOKE);
        put(Element.METAL, ParticleTypes.CRIT, ParticleTypes.ENCHANTED_HIT);
        put(Element.ECLIPSE, ParticleTypes.END_ROD, ParticleTypes.SMOKE);
        put(Element.BLOOD, AbilityUtil.BLOOD_DUST, ParticleTypes.CRIT);
        put(Element.CELESTIAL, ParticleTypes.END_ROD, ParticleTypes.FLAME);
        put(Element.GRAVITY, ParticleTypes.PORTAL, ParticleTypes.SQUID_INK);
        put(Element.TIME, ParticleTypes.ENCHANT, ParticleTypes.END_ROD);
        put(Element.REALITY_COLLAPSE, ParticleTypes.REVERSE_PORTAL, ParticleTypes.PORTAL);

        put(Element.ICE, ParticleTypes.SNOWFLAKE, ParticleTypes.CLOUD);
        put(Element.NATURE, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.SPORE_BLOSSOM_AIR);
        put(Element.POISON, ParticleTypes.WARPED_SPORE, ParticleTypes.SMOKE);
        put(Element.SAND, ParticleTypes.ASH, ParticleTypes.POOF);
        put(Element.STEAM, ParticleTypes.CLOUD, ParticleTypes.POOF);
        put(Element.MAGMA, ParticleTypes.LAVA, ParticleTypes.SMOKE);
        put(Element.STORM, ParticleTypes.CLOUD, ParticleTypes.ELECTRIC_SPARK);
        put(Element.CRYSTAL, ParticleTypes.END_ROD, ParticleTypes.ENCHANTED_HIT);
        put(Element.SOUND, ParticleTypes.NOTE, ParticleTypes.ENCHANT);
        put(Element.SHADOW, ParticleTypes.SQUID_INK, ParticleTypes.SMOKE);
        put(Element.ACID, ParticleTypes.COMPOSTER, ParticleTypes.SPLASH);
        put(Element.GLASS, ParticleTypes.END_ROD, ParticleTypes.CRIT);
        put(Element.SAKURA, ParticleTypes.CHERRY_LEAVES, ParticleTypes.HEART);
        put(Element.BONE, ParticleTypes.WHITE_ASH, ParticleTypes.CRIT);
        put(Element.SPIRIT, ParticleTypes.SOUL, ParticleTypes.SOUL_FIRE_FLAME);
        put(Element.PLASMA, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.FIREWORK);
        put(Element.DREAM, ParticleTypes.ENCHANT, ParticleTypes.END_ROD);
        put(Element.NIGHTMARE, ParticleTypes.SMOKE, ParticleTypes.CRIMSON_SPORE);
        put(Element.MIRROR, ParticleTypes.END_ROD, ParticleTypes.GLOW);
        put(Element.RUNE, ParticleTypes.ENCHANT, ParticleTypes.END_ROD);
        put(Element.COMET, ParticleTypes.END_ROD, ParticleTypes.FIREWORK);
        put(Element.COSMOS, ParticleTypes.END_ROD, ParticleTypes.PORTAL);
        put(Element.CHAOS, ParticleTypes.CRIMSON_SPORE, ParticleTypes.FLAME);
        put(Element.ORDER, ParticleTypes.END_ROD, ParticleTypes.FIREWORK);
        put(Element.LIFE, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.HEART);
        put(Element.DEATH, ParticleTypes.SOUL, ParticleTypes.ASH);
        put(Element.DRAGON, ParticleTypes.DRAGON_BREATH, ParticleTypes.FLAME);
        put(Element.VOID, ParticleTypes.PORTAL, ParticleTypes.REVERSE_PORTAL);
        put(Element.CREATION, ParticleTypes.END_ROD, ParticleTypes.FIREWORK);
        put(Element.ETERNITY, ParticleTypes.ENCHANT, ParticleTypes.END_ROD);
        put(Element.SOLAR, ParticleTypes.FLAME, ParticleTypes.END_ROD);
        put(Element.LUNAR, ParticleTypes.END_ROD, ParticleTypes.GLOW);
        put(Element.GOLD, ParticleTypes.FIREWORK, ParticleTypes.END_ROD);
        put(Element.WEB, ParticleTypes.WHITE_ASH, ParticleTypes.POOF);
        put(Element.RUST, ParticleTypes.ASH, ParticleTypes.CRIT);
        put(Element.ECHO, ParticleTypes.WITCH, ParticleTypes.GLOW);
    }


    // ------------------------------------------------------------------ modelo 3D do projetil de cada elemento
    /** shape/radius/height/style/color (color -1 = cor do elemento); peca 2 opcional (shape2 < 0 = nenhuma); back = aponta para tras (cauda). */
    public record ProjModel(int shape, float radius, float height, int style, int color,
                            int shape2, float radius2, float height2, int style2, int color2, boolean back) {}

    private static final ProjModel DEFAULT =
            new ProjModel(com.elementalawakening.entity.FxEntity.SPHERE, 0.4F, 0.0F,
                    com.elementalawakening.entity.FxEntity.PULSE, -1, -1, 0, 0, 0, 0, false);
    private static final Map<Element, ProjModel> MODELS = new EnumMap<>(Element.class);

    private static void model(Element e, int s, float r, float h, int st, int c) {
        MODELS.put(e, new ProjModel(s, r, h, st, c, -1, 0, 0, 0, 0, false));
    }

    private static void model2(Element e, int s, float r, float h, int st, int c,
                               int s2, float r2, float h2, int st2, int c2, boolean back) {
        MODELS.put(e, new ProjModel(s, r, h, st, c, s2, r2, h2, st2, c2, back));
    }

    public static ProjModel projModel(Element e) {
        ProjModel m = e == null ? null : MODELS.get(e);
        return m == null ? DEFAULT : m;
    }

    static {
        final int SPH = com.elementalawakening.entity.FxEntity.SPHERE, SPK = com.elementalawakening.entity.FxEntity.SPIKE,
                RNG = com.elementalawakening.entity.FxEntity.RING, DSC = com.elementalawakening.entity.FxEntity.DISC,
                BOX = com.elementalawakening.entity.FxEntity.BOX, ROK = com.elementalawakening.entity.FxEntity.ROCK;
        final int PUL = com.elementalawakening.entity.FxEntity.PULSE, SPN = com.elementalawakening.entity.FxEntity.SPIN,
                FLK = com.elementalawakening.entity.FxEntity.FLICKER, SOL = com.elementalawakening.entity.FxEntity.SOLID;

        // elementos da wiki
        model2(Element.FIRE, SPH, 0.5F, 0, FLK | PUL, 0xFF5A1F, SPK, 0.38F, 1.7F, FLK, 0xFFC83C, true);
        model2(Element.WATER, SPH, 0.45F, 0, PUL, 0x38D6FF, RNG, 0.58F, 0.05F, SPN, 0xFFFFFF, false);
        model2(Element.LIGHTNING, SPH, 0.3F, 0, FLK, 0xFFFFFF, RNG, 0.5F, 0.04F, SPN, 0x7FB4FF, false);
        model2(Element.EARTH, ROK, 0.5F, 0, SPN | SOL, 0xC9A66B, ROK, 0.25F, 0, SPN | SOL, 0x4A4458, false);
        model2(Element.WIND, RNG, 0.6F, 0.05F, SPN, 0xFFFFFF, RNG, 0.4F, 0.04F, SPN, 0xEFFFFF, false);
        model2(Element.LIGHT, SPH, 0.35F, 0, PUL, 0xFFFFFF, SPK, 0.3F, 2.0F, FLK, 0xFFFFC8, true);
        model2(Element.DARKNESS, SPH, 0.45F, 0, SOL | PUL, 0x120818, RNG, 0.55F, 0.05F, SPN, 0x9A55D8, false);
        model2(Element.METAL, BOX, 0.3F, 0.6F, SPN | SOL, 0x9AA1AB, SPK, 0.2F, 1.2F, 0, 0xD0D6DE, false);
        model2(Element.ECLIPSE, SPH, 0.4F, 0, SOL | PUL, 0x0A0F1A, RNG, 0.5F, 0.04F, SPN, 0xFFFFFF, false);
        model2(Element.BLOOD, SPH, 0.4F, 0, PUL, 0xE8405C, SPH, 0.22F, 0, SOL | PUL, 0x600818, false);
        model2(Element.CELESTIAL, SPH, 0.5F, 0, PUL, 0xFFC090, RNG, 0.7F, 0.04F, SPN, 0xFFFFFF, false);
        model2(Element.GRAVITY, SPH, 0.4F, 0, SOL | PUL, 0x2A0A40, RNG, 0.55F, 0.05F, SPN, 0xD060FF, false);
        model2(Element.TIME, RNG, 0.5F, 0.05F, SPN, 0xFFFFFF, DSC, 0.3F, 0.0F, SPN, 0xC8FF8C, false);
        model2(Element.REALITY_COLLAPSE, SPH, 0.45F, 0, SOL | PUL, 0x070A14, RNG, 0.55F, 0.05F, SPN, 0xFFFFFF, false);
        // elementos novos (mais)
        model2(Element.SOLAR, SPH, 0.55F, 0, FLK | PUL, 0xFFC93C, RNG, 0.7F, 0.05F, SPN, 0xFFFFFF, false);
        model2(Element.LUNAR, SPH, 0.4F, 0, PUL, 0xBFD0FF, RNG, 0.55F, 0.04F, SPN, 0xFFFFFF, false);
        model2(Element.GOLD, BOX, 0.18F, 0.5F, SPN | SOL, 0xFFD54A, SPH, 0.25F, 0, PUL, 0xFFF2B0, false);
        model2(Element.WEB, DSC, 0.55F, 0.0F, SPN, 0xE8E8F0, RNG, 0.5F, 0.03F, SPN, 0xFFFFFF, false);
        model2(Element.RUST, ROK, 0.45F, 0, SPN | SOL, 0xB5651D, ROK, 0.22F, 0, SPN | SOL, 0x7A3F10, false);
        model2(Element.ECHO, RNG, 0.5F, 0.05F, PUL, 0x9EE8FF, RNG, 0.3F, 0.04F, PUL, 0xFFFFFF, false);
        model2(Element.ICE, SPK, 0.22F, 1.4F, FLK, -1, SPH, 0.16F, 0, PUL, 0xFFFFFF, false);
        model2(Element.NATURE, SPK, 0.12F, 1.1F, SPN, -1, SPH, 0.2F, 0, PUL, 0xB8FF8C, false);
        model2(Element.POISON, SPH, 0.5F, 0, FLK | PUL, -1, SPH, 0.25F, 0, PUL, 0x6AA020, false);
        model2(Element.SAND, BOX, 0.28F, 0.55F, SPN | SOL, 0xE6C27A, BOX, 0.18F, 0.3F, SPN | SOL, 0xBFA060, false);
        model(Element.STEAM, SPH, 0.75F, 0, PUL, 0xFFFFFF);
        model2(Element.MAGMA, ROK, 0.5F, 0, SPN | SOL, 0xFF5A10, SPH, 0.65F, 0, FLK, 0xFF9A30, false);
        model2(Element.STORM, RNG, 0.55F, 0.06F, SPN, -1, SPH, 0.2F, 0, FLK, 0xDDF0FF, false);
        model2(Element.CRYSTAL, SPK, 0.2F, 1.3F, SPN, -1, SPK, 0.2F, 1.3F, SPN, 0xE8D8FF, true);
        model2(Element.SOUND, RNG, 0.6F, 0.05F, PUL, -1, RNG, 0.38F, 0.04F, PUL, 0xFFFFFF, false);
        model2(Element.SHADOW, SPH, 0.45F, 0, SOL | PUL, 0x120818, RNG, 0.5F, 0.05F, SPN, 0x9A70E0, false);
        model2(Element.ACID, SPH, 0.42F, 0, FLK, -1, SPH, 0.2F, 0, PUL, 0xE8FF70, false);
        model(Element.GLASS, SPK, 0.1F, 1.6F, SPN, -1);
        model2(Element.SAKURA, DSC, 0.5F, 0.0F, SPN, -1, SPH, 0.15F, 0, PUL, 0xFFFFFF, false);
        model(Element.BONE, BOX, 0.12F, 1.0F, SPN | SOL, 0xEDE6D0);
        model2(Element.SPIRIT, SPH, 0.4F, 0, PUL | FLK, -1, SPK, 0.25F, 1.4F, FLK, 0x8CFFE0, true);
        model2(Element.PLASMA, SPH, 0.5F, 0, PUL | FLK, -1, RNG, 0.6F, 0.05F, SPN, 0xFFFFFF, false);
        model2(Element.DREAM, SPH, 0.6F, 0, PUL, 0xFFB6F0, SPH, 0.3F, 0, PUL, 0xB0E0FF, false);
        model2(Element.NIGHTMARE, SPK, 0.3F, 1.5F, FLK | SPN, -1, SPH, 0.3F, 0, SOL | PUL, 0x1A0510, false);
        model2(Element.MIRROR, DSC, 0.6F, 0.0F, SPN, -1, RNG, 0.62F, 0.04F, 0, 0xFFFFFF, false);
        model2(Element.RUNE, RNG, 0.55F, 0.06F, SPN, -1, RNG, 0.35F, 0.05F, SPN, 0xFFFFFF, false);
        model2(Element.COMET, SPH, 0.35F, 0, PUL, 0xFFFFFF, SPK, 0.35F, 3.0F, FLK, -1, true);
        model2(Element.COSMOS, SPH, 0.35F, 0, PUL, -1, RNG, 0.7F, 0.04F, SPN, 0xFFFFFF, false);
        model2(Element.CHAOS, ROK, 0.45F, 0, SPN | SOL, 0xE03030, SPH, 0.7F, 0, FLK, -1, false);
        model2(Element.ORDER, BOX, 0.32F, 0.64F, SPN | SOL, 0xFFD84A, RNG, 0.6F, 0.04F, SPN, 0xFFFFFF, false);
        model2(Element.LIFE, SPH, 0.4F, 0, PUL, -1, SPH, 0.55F, 0, PUL | FLK, 0xFFFFFF, false);
        model2(Element.DEATH, SPK, 0.3F, 1.8F, SPN, 0xB8B8D8, SPK, 0.12F, 1.8F, 0, 0x606080, true);
        model2(Element.DRAGON, SPH, 0.5F, 0, FLK | PUL, -1, SPK, 0.4F, 2.5F, FLK, 0xFFD060, true);
        model2(Element.VOID, SPH, 0.45F, 0, SOL | PUL, 0x07040E, RNG, 0.5F, 0.05F, SPN, 0xFFFFFF, false);
        model2(Element.CREATION, SPH, 0.5F, 0, PUL, 0xFFFFFF, RNG, 0.7F, 0.05F, SPN, -1, false);
        model2(Element.ETERNITY, RNG, 0.7F, 0.06F, SPN, -1, SPH, 0.25F, 0, PUL, 0xFFFFFF, false);
    }

    private ElementFx() {}

    public static ParticleOptions p1(Element e) {
        ParticleOptions[] a = MAP.get(e);
        return a == null ? ParticleTypes.END_ROD : a[0];
    }

    public static ParticleOptions p2(Element e) {
        ParticleOptions[] a = MAP.get(e);
        return a == null ? ParticleTypes.ENCHANT : a[1];
    }

    /** Explosao de particulas do elemento (impactos). */
    public static void burst(ServerLevel level, Element e, Vec3 pos, float radius) {
        int n = Math.min(140, 24 + (int) (radius * 16));
        level.sendParticles(p1(e), pos.x, pos.y + 0.4, pos.z, n, radius * 0.4, radius * 0.25, radius * 0.4, 0.14);
        level.sendParticles(p2(e), pos.x, pos.y + 0.4, pos.z, n / 2, radius * 0.5, radius * 0.3, radius * 0.5, 0.06);
        if (radius >= 2.0F) {
            AbilityUtil.ring(level, pos, radius, p2(e));
            AbilityUtil.ring(level, pos.add(0, 0.6, 0), radius * 0.6, p1(e));
        }
    }

    /** Particulas de ambiente numa area (zonas, estruturas). */
    public static void ambient(ServerLevel level, Element e, Vec3 c, float radius) {
        level.sendParticles(p1(e), c.x, c.y + 0.5, c.z, 14, radius * 0.6, 0.35, radius * 0.6, 0.03);
        level.sendParticles(p2(e), c.x, c.y + 1.0, c.z, 8, radius * 0.6, 0.6, radius * 0.6, 0.02);
    }
}
