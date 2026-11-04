package com.elementalawakening.ability;

import com.elementalawakening.element.Element;
import com.elementalawakening.entity.FxEntity;
import com.elementalawakening.entity.MagicCircleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Cria os efeitos 3D (FxEntity). "signature" monta a estrutura de cada elemento,
 * inspirada nos prints do jogo original.
 */
public final class Fx {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int WHITE = 0xFFFFFF;

    private Fx() {}

    public static FxEntity make(ServerLevel level, int shape, Vec3 pos, Vec3 dir, int color,
                                float radius, float height, int life, int style) {
        return FxEntity.spawn(level, shape, pos, dir, color, radius, height, life, style);
    }

    /** Onda de choque: anel + cupula que se expandem. */
    public static void shockwave(ServerLevel level, Vec3 ground, int color, float radius) {
        make(level, FxEntity.RING, ground.add(0, 0.15, 0), UP, color, radius, 0.12F + radius * 0.02F, 10, FxEntity.EXPAND);
        make(level, FxEntity.DOME, ground, UP, color, radius * 0.9F, 0, 8, FxEntity.EXPAND);
    }

    private static Vec3 off(Vec3 g, double r, double angDeg, double y) {
        double a = Math.toRadians(angDeg);
        return g.add(Math.cos(a) * r, y, Math.sin(a) * r);
    }

    private static Vec3 rndDir(RandomSource rnd, double up) {
        return new Vec3(rnd.nextFloat() - 0.5, up, rnd.nextFloat() - 0.5);
    }


    // ------------------------------------------------------------------ elementos novos: familias de estrutura 3D
    private enum Family { CRYSTALS, ORBIT, VORTEX, PILLAR_RINGS, DOME_FIELD, SHARDS, PETALS, TENDRILS, STAR, GEOMETRY }

    private record Sig(Family family, int c1, int c2) {}

    private static final java.util.Map<Element, Sig> SIGS = new java.util.EnumMap<>(Element.class);

    private static void sig(Element e, Family f, int c1, int c2) {
        SIGS.put(e, new Sig(f, c1, c2));
    }

    static {
        sig(Element.ICE, Family.CRYSTALS, 0xA8E8FF, 0xFFFFFF);
        sig(Element.NATURE, Family.TENDRILS, 0x5EDB6A, 0xB8FF8C);
        sig(Element.POISON, Family.DOME_FIELD, 0x9BE03A, 0x6AA020);
        sig(Element.SAND, Family.VORTEX, 0xE6C27A, 0xFFE9B0);
        sig(Element.STEAM, Family.DOME_FIELD, 0xDDE8EE, 0xFFFFFF);
        sig(Element.MAGMA, Family.PILLAR_RINGS, 0xFF6B1A, 0xFFC040);
        sig(Element.STORM, Family.VORTEX, 0x6C8CFF, 0xDDF0FF);
        sig(Element.CRYSTAL, Family.CRYSTALS, 0xB48CFF, 0xE8D8FF);
        sig(Element.SOUND, Family.PILLAR_RINGS, 0xFF9EDC, 0xFFFFFF);
        sig(Element.SHADOW, Family.TENDRILS, 0x7A56C8, 0x3A2070);
        sig(Element.ACID, Family.DOME_FIELD, 0xC8FF2E, 0x88C020);
        sig(Element.GLASS, Family.CRYSTALS, 0xBFF4FF, 0xFFFFFF);
        sig(Element.SAKURA, Family.PETALS, 0xFFA6C9, 0xFFFFFF);
        sig(Element.BONE, Family.SHARDS, 0xEDE6D0, 0xBFB59A);
        sig(Element.SPIRIT, Family.ORBIT, 0x8CFFE0, 0xFFFFFF);
        sig(Element.PLASMA, Family.STAR, 0xFF4DFF, 0xFFFFFF);
        sig(Element.DREAM, Family.PETALS, 0xFFB6F0, 0xB0E0FF);
        sig(Element.NIGHTMARE, Family.TENDRILS, 0xC0306A, 0x400020);
        sig(Element.MIRROR, Family.GEOMETRY, 0xE0F0FF, 0xFFFFFF);
        sig(Element.RUNE, Family.GEOMETRY, 0xFFD060, 0xFFFFFF);
        sig(Element.COMET, Family.PILLAR_RINGS, 0x80D8FF, 0xFFFFFF);
        sig(Element.COSMOS, Family.ORBIT, 0x6A5CFF, 0xFFFFFF);
        sig(Element.CHAOS, Family.SHARDS, 0xFF5050, 0xFFA050);
        sig(Element.ORDER, Family.GEOMETRY, 0xFFE070, 0xFFFFFF);
        sig(Element.LIFE, Family.PETALS, 0x7CFF9A, 0xFFFFFF);
        sig(Element.DEATH, Family.TENDRILS, 0xB8B8D8, 0x606080);
        sig(Element.DRAGON, Family.VORTEX, 0xFF8A3C, 0xFFD060);
        sig(Element.VOID, Family.DOME_FIELD, 0xA070FF, 0x401080);
        sig(Element.CREATION, Family.STAR, 0xFFF2A0, 0xFFFFFF);
        sig(Element.ETERNITY, Family.GEOMETRY, 0xA0FFF0, 0xFFFFFF);
        sig(Element.SOLAR, Family.STAR, 0xFFC93C, 0xFFFFFF);
        sig(Element.LUNAR, Family.ORBIT, 0xBFD0FF, 0xFFFFFF);
        sig(Element.GOLD, Family.CRYSTALS, 0xFFD54A, 0xFFF2B0);
        sig(Element.WEB, Family.TENDRILS, 0xE8E8F0, 0x9090A0);
        sig(Element.RUST, Family.SHARDS, 0xB5651D, 0x7A3F10);
        sig(Element.ECHO, Family.PILLAR_RINGS, 0x9EE8FF, 0xFFFFFF);
    }

    private static void family(ServerLevel level, Vec3 g, Element el, float R, int L, RandomSource rnd) {
        Sig s = SIGS.get(el);
        if (s == null) return;
        int c1 = s.c1(), c2 = s.c2();
        Vec3 mid = g.add(0, R * 0.9, 0);

        switch (s.family()) {
            case CRYSTALS -> {
                make(level, FxEntity.SPIKE, g, UP, c1, R * 0.2F, R * 1.8F, L, FxEntity.RISE);
                for (int k = 0; k < 8; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.7, k * 45 + rnd.nextInt(15), 0), rndDir(rnd, 1.4),
                            k % 2 == 0 ? c1 : c2, R * 0.12F, R * (0.7F + rnd.nextFloat() * 0.8F), L, FxEntity.RISE);
                }
                make(level, FxEntity.DOME, g, UP, c2, R * 0.7F, 0, L, FxEntity.PULSE);
            }
            case ORBIT -> {
                make(level, FxEntity.SPHERE, mid, UP, c1, R * 0.3F, 0, L, FxEntity.PULSE);
                make(level, FxEntity.RING, mid, new Vec3(0.6, 1, 0), c2, R * 0.55F, 0.05F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, mid, new Vec3(-0.5, 1, 0.5), c2, R * 0.7F, 0.04F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, mid, new Vec3(0.2, 1, -0.7), c2, R * 0.85F, 0.035F, L, FxEntity.SPIN);
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.SPHERE, off(mid, R * 0.7, k * 90, (rnd.nextFloat() - 0.5) * R * 0.5), UP, c2,
                            0.14F, 0, L, FxEntity.PULSE);
                }
            }
            case VORTEX -> {
                make(level, FxEntity.FUNNEL, g, UP, c1, R * 0.85F, R * 2.3F, L, FxEntity.SPIN | FxEntity.PULSE);
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, c2, R * 0.9F, 0.08F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, g.add(0, R * 0.9, 0), UP, c2, R * 0.6F, 0.06F, L, FxEntity.SPIN);
            }
            case PILLAR_RINGS -> {
                make(level, FxEntity.PILLAR, g, UP, c1, R * 0.3F, R * 2.4F, L, FxEntity.RISE);
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.RING, g.add(0, 0.1, 0), UP, c2, R * (0.4F + 0.25F * k), 0.05F, L, FxEntity.EXPAND);
                }
            }
            case DOME_FIELD -> {
                make(level, FxEntity.DOME, g, UP, c1, R * 1.0F, 0, L, FxEntity.PULSE);
                for (int k = 0; k < 8; k++) {
                    Vec3 o = AbilityUtil.randomInDisc(rnd, g, R * 0.8).add(0, 0.4 + rnd.nextFloat() * R * 0.9, 0);
                    make(level, FxEntity.SPHERE, o, UP, c2, 0.1F + rnd.nextFloat() * 0.15F, 0, L, FxEntity.PULSE | FxEntity.FLICKER);
                }
            }
            case SHARDS -> {
                for (int k = 0; k < 9; k++) {
                    Vec3 o = AbilityUtil.randomInDisc(rnd, g, R * 0.85).add(0, rnd.nextFloat() * R * 0.7, 0);
                    float half = 0.15F + rnd.nextFloat() * 0.25F;
                    make(level, FxEntity.BOX, o, rndDir(rnd, 1), c1, half, half * 2.5F, L, FxEntity.SOLID);
                }
                for (int k = 0; k < 5; k++) {
                    Vec3 o = AbilityUtil.randomInDisc(rnd, g, R * 0.8).add(0, R * (0.3 + rnd.nextFloat()), 0);
                    make(level, FxEntity.ROCK, o, UP, c2, 0.2F + rnd.nextFloat() * 0.2F, 0, L, FxEntity.SOLID);
                }
                make(level, FxEntity.RING, g.add(0, 0.1, 0), UP, c1, R * 0.9F, 0.06F, L, 0);
            }
            case PETALS -> {
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, c1, R * 0.8F, 0.0F, L, FxEntity.PULSE);
                for (int k = 0; k < 14; k++) {
                    double ang = k * 36 + rnd.nextInt(20);
                    Vec3 o = off(g, R * (0.3 + 0.5 * (k % 5) / 4.0), ang, 0.4 + k * R * 0.07);
                    make(level, FxEntity.SPHERE, o, UP, k % 2 == 0 ? c1 : c2, 0.12F + rnd.nextFloat() * 0.1F, 0, L,
                            FxEntity.PULSE | FxEntity.FLICKER);
                }
            }
            case TENDRILS -> {
                for (int k = 0; k < 7; k++) {
                    double a = Math.toRadians(k * 51 + rnd.nextInt(20));
                    make(level, FxEntity.BEAM, g.add(0, 0.2, 0), new Vec3(Math.cos(a) * 0.6, 1, Math.sin(a) * 0.6), c1,
                            0.07F, R * (1.4F + rnd.nextFloat()), L, FxEntity.FLICKER | FxEntity.RISE);
                }
                make(level, FxEntity.SPHERE, g.add(0, R * 0.5, 0), UP, c2, R * 0.25F, 0, L, FxEntity.PULSE);
            }
            case STAR -> {
                make(level, FxEntity.SPHERE, mid, UP, c1, R * 0.3F, 0, L, FxEntity.PULSE);
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(k * 45);
                    make(level, FxEntity.BEAM, mid, new Vec3(Math.cos(a), 0, Math.sin(a)), c2, 0.06F, R * 0.95F, L, FxEntity.FLICKER);
                }
                make(level, FxEntity.RING, mid, UP, c1, R * 0.6F, 0.05F, L, FxEntity.SPIN);
            }
            case GEOMETRY -> {
                make(level, FxEntity.RING, mid, new Vec3(1, 0, 0), c1, R * 0.6F, 0.05F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, mid, new Vec3(0, 0, 1), c1, R * 0.6F, 0.05F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, mid, new Vec3(1, 0, 1), c1, R * 0.45F, 0.04F, L, FxEntity.SPIN);
                make(level, FxEntity.SPHERE, mid, UP, c2, R * 0.12F, 0, L, FxEntity.PULSE);
                make(level, FxEntity.RING, g.add(0, 0.1, 0), UP, c2, R * 0.9F, 0.05F, L, FxEntity.EXPAND);
            }
        }
    }


    private static Vec3 outUp(double ang, double up) {
        double a = Math.toRadians(ang);
        return new Vec3(Math.cos(a) * 0.8, up, Math.sin(a) * 0.8);
    }

    /** Impacto proprio de cada elemento (curto). Elementos da wiki usam a onda de choque padrao. */
    public static void impact(ServerLevel level, Vec3 g, Element el, float radius) {
        RandomSource rnd = level.getRandom();
        float R = Math.max(1.0F, radius);
        int c = el.color;
        int L = 14;

        switch (el) {
            case ICE -> {
                for (int k = 0; k < 8; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.3, k * 45, 0), outUp(k * 45, 1), c, R * 0.07F, R * (0.6F + rnd.nextFloat() * 0.5F), L, FxEntity.RISE);
                }
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, WHITE, R, 0.08F, 10, FxEntity.EXPAND);
            }
            case NATURE -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.BEAM, off(g, R * 0.4, k * 60, 0), outUp(k * 60, 1.6), c, 0.06F, R * (0.8F + rnd.nextFloat() * 0.6F), 18, FxEntity.RISE);
                }
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, c, R * 0.9F, 0.0F, 16, FxEntity.EXPAND);
            }
            case POISON -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, g, R * 0.6).add(0, 0.5 + rnd.nextFloat() * R * 0.6, 0), UP, c, 0.2F + rnd.nextFloat() * 0.3F, 0, 16, FxEntity.PULSE);
                }
                make(level, FxEntity.DOME, g, UP, c, R * 0.8F, 0, 12, FxEntity.EXPAND);
            }
            case SAND -> {
                make(level, FxEntity.FUNNEL, g, UP, c, R * 0.7F, R * 1.6F, 16, FxEntity.SPIN | FxEntity.EXPAND);
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, 0xFFE9B0, R, 0.1F, 12, FxEntity.EXPAND);
            }
            case STEAM -> {
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, g, R * 0.5).add(0, 0.6 + k * 0.4, 0), UP, WHITE, R * 0.3F, 0, 18, FxEntity.PULSE | FxEntity.EXPAND);
                }
                make(level, FxEntity.DOME, g, UP, c, R * 0.9F, 0, 14, FxEntity.EXPAND);
            }
            case MAGMA -> {
                for (int k = 0; k < 5; k++) {
                    make(level, FxEntity.PILLAR, off(g, R * 0.45, k * 72, 0), UP, 0xFF6B1A, R * 0.12F, R * (0.8F + rnd.nextFloat() * 0.8F), L, FxEntity.RISE | FxEntity.FLICKER);
                }
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, 0xFFC040, R, 0.1F, 12, FxEntity.EXPAND);
            }
            case STORM -> {
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.PILLAR, AbilityUtil.randomInDisc(rnd, g, R * 0.6), UP, 0xDDF0FF, 0.1F, 8.0F, 8, FxEntity.FLICKER);
                }
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, c, R, 0.08F, 10, FxEntity.EXPAND);
            }
            case CRYSTAL -> {
                for (int k = 0; k < 8; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.5, k * 45 + 22, 0), outUp(k * 45 + 22, 1.5), k % 2 == 0 ? c : 0xE8D8FF, R * 0.09F, R * (0.9F + rnd.nextFloat() * 0.7F), L, FxEntity.RISE);
                }
            }
            case SOUND -> {
                make(level, FxEntity.RING, g.add(0, 0.5, 0), UP, c, R * 0.5F, 0.06F, 12, FxEntity.EXPAND);
                make(level, FxEntity.RING, g.add(0, 0.5, 0), UP, WHITE, R * 0.75F, 0.05F, 12, FxEntity.EXPAND);
                make(level, FxEntity.RING, g.add(0, 0.5, 0), UP, c, R, 0.04F, 12, FxEntity.EXPAND);
            }
            case SHADOW -> {
                make(level, FxEntity.DOME, g, UP, c, R * 0.8F, 0, 14, FxEntity.EXPAND);
                for (int k = 0; k < 5; k++) {
                    make(level, FxEntity.BEAM, off(g, R * 0.2, k * 72, 0), outUp(k * 72, 1.2), 0x3A2070, 0.07F, R, 14, FxEntity.RISE | FxEntity.FLICKER);
                }
            }
            case ACID -> {
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, c, R * 0.9F, 0.0F, 18, FxEntity.EXPAND);
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, g, R * 0.5).add(0, 0.4, 0), UP, 0xE8FF70, 0.18F, 0, 14, FxEntity.PULSE | FxEntity.FLICKER);
                }
            }
            case GLASS -> {
                for (int k = 0; k < 7; k++) {
                    make(level, FxEntity.BOX, AbilityUtil.randomInDisc(rnd, g, R * 0.6).add(0, rnd.nextFloat() * R * 0.5, 0), rndDir(rnd, 1), 0xBFF4FF, 0.07F, 0.4F + rnd.nextFloat() * 0.4F, 12, FxEntity.SOLID);
                }
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, WHITE, R, 0.05F, 10, FxEntity.EXPAND);
            }
            case SAKURA -> {
                for (int k = 0; k < 10; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, g, R * 0.8).add(0, 0.3 + rnd.nextFloat() * R * 0.7, 0), UP, k % 2 == 0 ? c : WHITE, 0.1F, 0, 18, FxEntity.FLICKER);
                }
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, c, R * 0.8F, 0.0F, 14, FxEntity.EXPAND);
            }
            case BONE -> {
                for (int k = 0; k < 5; k++) {
                    make(level, FxEntity.BOX, off(g, R * 0.4, k * 72, 0), UP, 0xEDE6D0, 0.1F, R * (0.9F + rnd.nextFloat() * 0.6F), L, FxEntity.RISE | FxEntity.SOLID);
                }
            }
            case SPIRIT -> {
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.35, k * 120, 0), UP, c, 0.18F, R * 1.4F, 18, FxEntity.RISE | FxEntity.FLICKER);
                }
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, WHITE, R * 0.8F, 0.06F, 12, FxEntity.EXPAND);
            }
            case PLASMA -> {
                for (int k = 0; k < 6; k++) {
                    double a = Math.toRadians(k * 60);
                    make(level, FxEntity.BEAM, g.add(0, 0.8, 0), new Vec3(Math.cos(a), 0, Math.sin(a)), WHITE, 0.07F, R, 8, FxEntity.FLICKER);
                }
                make(level, FxEntity.SPHERE, g.add(0, 0.8, 0), UP, c, R * 0.3F, 0, 8, FxEntity.PULSE);
            }
            case DREAM -> {
                make(level, FxEntity.RING, g.add(0, 0.4, 0), UP, c, R, 0.07F, 14, FxEntity.EXPAND);
                for (int k = 0; k < 5; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, g, R * 0.6).add(0, 0.6 + rnd.nextFloat() * R * 0.8, 0), UP, 0xB0E0FF, 0.1F, 0, 18, FxEntity.PULSE | FxEntity.FLICKER);
                }
            }
            case NIGHTMARE -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.4, k * 60, 0), outUp(k * 60, 1.2), c, 0.14F, R * (0.8F + rnd.nextFloat() * 0.5F), L, FxEntity.RISE | FxEntity.FLICKER);
                }
            }
            case MIRROR -> {
                make(level, FxEntity.RING, g.add(0, 0.8, 0), new Vec3(1, 0, 0), WHITE, R * 0.5F, 0.04F, 10, FxEntity.SPIN);
                make(level, FxEntity.RING, g.add(0, 0.8, 0), new Vec3(0, 0, 1), c, R * 0.5F, 0.04F, 10, FxEntity.SPIN);
                make(level, FxEntity.SPHERE, g.add(0, 0.8, 0), UP, WHITE, R * 0.2F, 0, 8, FxEntity.PULSE);
            }
            case RUNE -> {
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, c, R * 0.6F, 0.06F, 14, FxEntity.EXPAND);
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, WHITE, R, 0.04F, 14, FxEntity.EXPAND);
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.BOX, off(g, R * 0.6, k * 90 + 45, 0), UP, c, 0.1F, R * 0.7F, 14, FxEntity.RISE | FxEntity.SOLID);
                }
            }
            case COMET -> {
                make(level, FxEntity.SPHERE, g.add(0, 0.6, 0), UP, WHITE, R * 0.3F, 0, 8, FxEntity.PULSE);
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.BEAM, g.add(0, 0.3, 0), outUp(k * 60, 1.3), c, 0.05F, R * 1.2F, 10, FxEntity.FLICKER);
                }
            }
            case COSMOS -> {
                make(level, FxEntity.SPHERE, g.add(0, 0.8, 0), UP, c, R * 0.25F, 0, 12, FxEntity.PULSE);
                make(level, FxEntity.RING, g.add(0, 0.8, 0), new Vec3(0.5, 1, 0), WHITE, R * 0.6F, 0.04F, 14, FxEntity.SPIN | FxEntity.EXPAND);
                make(level, FxEntity.RING, g.add(0, 0.8, 0), new Vec3(-0.5, 1, 0.4), c, R * 0.9F, 0.035F, 14, FxEntity.SPIN | FxEntity.EXPAND);
            }
            case CHAOS -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.ROCK, AbilityUtil.randomInDisc(rnd, g, R * 0.6).add(0, 0.3 + rnd.nextFloat() * R * 0.6, 0), UP, 0xE03030, 0.2F + rnd.nextFloat() * 0.2F, 0, 12, FxEntity.SOLID);
                }
                make(level, FxEntity.SPHERE, g.add(0, 0.7, 0), UP, c, R * 0.25F, 0, 8, FxEntity.FLICKER);
            }
            case ORDER -> {
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.BOX, off(g, R * 0.6, k * 90 + 45, 0), UP, 0xFFD84A, 0.12F, R * 1.1F, 14, FxEntity.RISE | FxEntity.SOLID);
                }
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, WHITE, R * 0.8F, 0.05F, 12, FxEntity.EXPAND);
            }
            case LIFE -> {
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, c, R * 0.9F, 0.0F, 16, FxEntity.EXPAND);
                for (int k = 0; k < 8; k++) {
                    make(level, FxEntity.SPHERE, off(g, R * 0.5, k * 45, 0.3 + k * 0.15), UP, k % 2 == 0 ? c : WHITE, 0.1F, 0, 18, FxEntity.FLICKER);
                }
            }
            case DEATH -> {
                make(level, FxEntity.DOME, g, UP, c, R * 0.9F, 0, 14, FxEntity.EXPAND | FxEntity.FLICKER);
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.4, k * 90, 0), UP, 0xB8B8D8, 0.12F, R * 1.2F, L, FxEntity.RISE);
                }
            }
            case DRAGON -> {
                make(level, FxEntity.PILLAR, g, UP, c, R * 0.35F, R * 1.8F, L, FxEntity.RISE | FxEntity.FLICKER);
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, 0xFFD060, R, 0.1F, 12, FxEntity.EXPAND);
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.5, k * 90 + 45, 0), outUp(k * 90 + 45, 1.2), c, 0.14F, R, L, FxEntity.RISE);
                }
            }
            case VOID -> {
                make(level, FxEntity.SPHERE, g.add(0, 0.9, 0), UP, 0x07040E, R * 0.3F, 0, 14, FxEntity.SOLID | FxEntity.PULSE);
                make(level, FxEntity.RING, g.add(0, 0.9, 0), new Vec3(1, 0.3, 0), WHITE, R * 0.33F, 0.04F, 14, FxEntity.SPIN);
                make(level, FxEntity.DOME, g, UP, c, R * 0.8F, 0, 12, FxEntity.EXPAND);
            }
            case CREATION -> {
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(k * 45);
                    make(level, FxEntity.BEAM, g.add(0, 0.8, 0), new Vec3(Math.cos(a), 0.3, Math.sin(a)), WHITE, 0.06F, R, 10, FxEntity.FLICKER);
                }
                make(level, FxEntity.SPHERE, g.add(0, 0.8, 0), UP, c, R * 0.3F, 0, 10, FxEntity.PULSE);
            }
            case ETERNITY -> {
                make(level, FxEntity.RING, g.add(0, 0.8, 0), new Vec3(1, 0, 0), c, R * 0.6F, 0.05F, 16, FxEntity.SPIN | FxEntity.EXPAND);
                make(level, FxEntity.RING, g.add(0, 0.8, 0), new Vec3(0, 0, 1), WHITE, R * 0.6F, 0.05F, 16, FxEntity.SPIN | FxEntity.EXPAND);
                make(level, FxEntity.RING, g.add(0, 0.8, 0), new Vec3(1, 0, 1), c, R * 0.9F, 0.04F, 16, FxEntity.SPIN | FxEntity.EXPAND);
            }
            default -> shockwave(level, g, c, R);
        }
    }

    /** Pecas extras exclusivas de cada elemento novo (somam com a estrutura da familia). */
    private static void extras(ServerLevel level, Vec3 g, Element el, float R, int L, RandomSource rnd) {
        int c = el.color;
        Vec3 mid = g.add(0, R * 0.9, 0);
        switch (el) {
            case ICE -> {
                for (int k = 0; k < 10; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, g, R).add(0, rnd.nextFloat() * R * 2, 0), UP, WHITE, 0.07F, 0, L, FxEntity.FLICKER);
                }
                make(level, FxEntity.RING, g.add(0, 0.1, 0), UP, WHITE, R * 0.95F, 0.05F, L, FxEntity.EXPAND);
            }
            case NATURE -> {
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.SPHERE, off(g, R * 0.5, k * 90 + 20, R * (1.3 + rnd.nextFloat() * 0.4)), UP, 0xFF9AD0, 0.22F, 0, L, FxEntity.PULSE);
                }
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, c, R * 0.9F, 0.0F, L, 0);
            }
            case POISON -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, g, R * 0.6).add(0, R * (0.6 + rnd.nextFloat()), 0), UP, c, 0.2F + rnd.nextFloat() * 0.25F, 0, L, FxEntity.PULSE | FxEntity.FLICKER);
                }
            }
            case SAND -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.BOX, AbilityUtil.randomInDisc(rnd, g, R * 0.9).add(0, 0.3 + rnd.nextFloat() * R, 0), rndDir(rnd, 1), 0xE6C27A, 0.18F, 0.36F, L, FxEntity.SOLID | FxEntity.SPIN);
                }
            }
            case STEAM -> {
                for (int k = 0; k < 5; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, g, R * 0.6).add(0, R * (0.3 + k * 0.3), 0), UP, WHITE, R * 0.25F, 0, L, FxEntity.PULSE);
                }
            }
            case MAGMA -> {
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, 0xFF3A10, R * 0.9F, 0.0F, L, FxEntity.PULSE);
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.ROCK, AbilityUtil.randomInDisc(rnd, g, R * 0.8).add(0, R * (0.4 + rnd.nextFloat()), 0), UP, 0x802010, 0.22F, 0, L, FxEntity.SOLID);
                }
            }
            case STORM -> {
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.PILLAR, AbilityUtil.randomInDisc(rnd, g, R * 0.7), UP, 0xEAF4FF, 0.1F, 12.0F, Math.min(L, 24), FxEntity.FLICKER);
                }
            }
            case CRYSTAL -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.SPIKE, AbilityUtil.randomInDisc(rnd, g, R * 0.9).add(0, R * (0.5 + rnd.nextFloat()), 0), UP, 0xE8D8FF, 0.1F, 0.7F, L, FxEntity.FLICKER);
                }
            }
            case SOUND -> {
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.RING, g.add(0, R * (0.4 + 0.4 * k), 0), UP, k % 2 == 0 ? c : WHITE, R * 0.5F, 0.04F, L, FxEntity.SPIN | FxEntity.PULSE);
                }
            }
            case SHADOW -> {
                make(level, FxEntity.SPHERE, mid, UP, 0x120818, R * 0.22F, 0, L, FxEntity.SOLID | FxEntity.PULSE);
                make(level, FxEntity.RING, mid, new Vec3(0.3, 1, 0.2), 0x9A70E0, R * 0.3F, 0.04F, L, FxEntity.SPIN);
            }
            case ACID -> {
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, c, R * 0.9F, 0.0F, L, FxEntity.PULSE);
                for (int k = 0; k < 5; k++) {
                    make(level, FxEntity.PILLAR, AbilityUtil.randomInDisc(rnd, g, R * 0.7).add(0, R, 0), new Vec3(0, -1, 0), 0xE8FF70, 0.05F, R * 0.8F, L, FxEntity.FLICKER);
                }
            }
            case GLASS -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.BOX, AbilityUtil.randomInDisc(rnd, g, R * 0.8).add(0, R * (0.2 + rnd.nextFloat() * 0.8), 0), rndDir(rnd, 1), 0xBFF4FF, 0.08F, 0.6F, L, FxEntity.SOLID);
                }
            }
            case SAKURA -> {
                make(level, FxEntity.RING, g.add(0, R * 0.6, 0), UP, 0xFFFFFF, R * 0.7F, 0.04F, L, FxEntity.SPIN);
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, c, R * 1.0F, 0.0F, L, 0);
            }
            case BONE -> {
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.BOX, off(g, R * 0.55, k * 90, 0), UP, 0xEDE6D0, 0.1F, R * 1.4F, L, FxEntity.RISE | FxEntity.SOLID);
                }
                make(level, FxEntity.ROCK, g.add(0, R * 1.9, 0), UP, 0xEDE6D0, R * 0.2F, 0, L, FxEntity.SOLID);
            }
            case SPIRIT -> {
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.4, k * 120 + 30, 0), UP, 0xFFFFFF, 0.14F, R * 1.5F, L, FxEntity.RISE | FxEntity.FLICKER);
                }
            }
            case PLASMA -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.BEAM, mid, rndDir(rnd, 0.6), WHITE, 0.04F, R * 1.1F, L, FxEntity.FLICKER);
                }
            }
            case DREAM -> {
                make(level, FxEntity.RING, mid.add(0, R * 0.3, 0), new Vec3(0.2, 1, 0.5), 0xFFFFFF, R * 0.55F, 0.05F, L, FxEntity.SPIN);
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, mid, R * 0.9).add(0, rnd.nextFloat() * R * 0.8, 0), UP, 0xFFFFFF, 0.08F, 0, L, FxEntity.PULSE | FxEntity.FLICKER);
                }
            }
            case NIGHTMARE -> {
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.5, k * 90, R * 2.0), new Vec3(0, -1, 0), c, 0.14F, R * 0.9F, L, FxEntity.RISE | FxEntity.FLICKER);
                }
            }
            case MIRROR -> {
                make(level, FxEntity.DISC, mid, new Vec3(0, 0, 1), WHITE, R * 0.5F, 0.0F, L, FxEntity.PULSE);
                make(level, FxEntity.DISC, mid, new Vec3(1, 0, 0), c, R * 0.5F, 0.0F, L, FxEntity.PULSE);
            }
            case RUNE -> {
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.BOX, off(g, R * 0.85, k * 90 + 45, 0), UP, c, 0.15F, R * 0.9F, L, FxEntity.RISE | FxEntity.SOLID);
                }
            }
            case COMET -> {
                make(level, FxEntity.SPHERE, g.add(0, R * 2.4, 0), UP, WHITE, R * 0.22F, 0, L, FxEntity.PULSE);
                make(level, FxEntity.BEAM, g.add(0, R * 2.4, 0), new Vec3(0.4, -1, 0.2), c, 0.12F, R * 2.2F, L, FxEntity.FLICKER);
            }
            case COSMOS -> {
                for (int k = 0; k < 8; k++) {
                    make(level, FxEntity.SPHERE, AbilityUtil.randomInDisc(rnd, g, R * 1.2).add(0, R * (0.3 + rnd.nextFloat() * 1.6), 0), UP, WHITE, 0.07F, 0, L, FxEntity.FLICKER);
                }
                make(level, FxEntity.RING, mid, new Vec3(0.4, 1, 0.1), c, R * 1.0F, 0.04F, L, FxEntity.SPIN);
            }
            case CHAOS -> {
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.ROCK, AbilityUtil.randomInDisc(rnd, g, R * 0.9).add(0, R * (0.2 + rnd.nextFloat()), 0), UP, 0xE03030, 0.2F + rnd.nextFloat() * 0.2F, 0, L, FxEntity.SOLID);
                }
            }
            case ORDER -> {
                for (int k = 0; k < 4; k++) {
                    make(level, FxEntity.BOX, off(g, R * 0.8, k * 90 + 45, 0), UP, 0xFFD84A, 0.14F, R * 1.8F, L, FxEntity.RISE | FxEntity.SOLID);
                }
                make(level, FxEntity.RING, g.add(0, R * 1.8, 0), UP, WHITE, R * 1.1F, 0.04F, L, FxEntity.SPIN);
            }
            case LIFE -> {
                make(level, FxEntity.PILLAR, g, UP, 0x8A5A2B, R * 0.12F, R * 1.4F, L, FxEntity.RISE);
                make(level, FxEntity.SPHERE, g.add(0, R * 1.6, 0), UP, c, R * 0.5F, 0, L, FxEntity.PULSE);
            }
            case DEATH -> {
                make(level, FxEntity.DOME, g, UP, 0x606080, R * 1.1F, 0, L, FxEntity.FLICKER);
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.ROCK, AbilityUtil.randomInDisc(rnd, g, R * 0.7).add(0, R * (0.4 + rnd.nextFloat()), 0), UP, 0x303040, 0.25F, 0, L, FxEntity.SOLID);
                }
            }
            case DRAGON -> {
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.6, k * 120, 0), outUp(k * 120, 1.6), 0xFF8A3C, R * 0.12F, R * 1.6F, L, FxEntity.RISE | FxEntity.FLICKER);
                }
                make(level, FxEntity.RING, g.add(0, 0.15, 0), UP, 0xFFD060, R * 0.95F, 0.08F, L, FxEntity.SPIN);
            }
            case VOID -> {
                make(level, FxEntity.SPHERE, mid, UP, 0x07040E, R * 0.35F, 0, L, FxEntity.SOLID | FxEntity.PULSE);
                make(level, FxEntity.DISC, mid, UP, c, R * 0.95F, R * 0.4F, L, FxEntity.SPIN);
            }
            case CREATION -> {
                make(level, FxEntity.BEAM, g, UP, c, R * 0.12F, 22.0F, L, FxEntity.FLICKER);
                for (int k = 0; k < 6; k++) {
                    make(level, FxEntity.SPHERE, off(g, R * 0.6, k * 60, 0.5 + k * R * 0.3), UP, WHITE, 0.14F, 0, L, FxEntity.PULSE);
                }
            }
            case ETERNITY -> {
                make(level, FxEntity.RING, mid.add(R * 0.35, 0, 0), new Vec3(0, 0, 1), c, R * 0.35F, 0.05F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, mid.add(-R * 0.35, 0, 0), new Vec3(0, 0, 1), WHITE, R * 0.35F, 0.05F, L, FxEntity.SPIN);
            }
            default -> { }
        }
    }

    /** Estrutura 3D do elemento, no chao 'g'. size = tamanho do circulo; life em ticks. */
    public static void signature(ServerLevel level, Vec3 g, Element el, float size, int life) {
        RandomSource rnd = level.getRandom();
        float R = size * 0.85F;
        int L = Math.max(10, life);
        int c = el.color;
        Vec3 mid = g.add(0, R * 0.9, 0);

        // particulas de ambiente durante a vida da estrutura
        Scheduler.repeat(Math.min(L / 6, 30), 6, i -> ElementFx.ambient(level, el, g, R));

        switch (el) {
            case FIRE -> {
                // chamas vermelho-alaranjadas com miolo amarelo + brasas
                make(level, FxEntity.PILLAR, g, UP, 0xFF4A1A, R * 0.5F, R * 2.6F, L, FxEntity.RISE | FxEntity.FLICKER);
                make(level, FxEntity.PILLAR, g, UP, 0xFFC83C, R * 0.28F, R * 2.2F, L, FxEntity.RISE | FxEntity.FLICKER);
                for (int k = 0; k < 5; k++) {
                    make(level, FxEntity.SPIKE, off(g, R * 0.75, k * 72 + rnd.nextInt(20), 0), UP, 0xFF7A1F,
                            R * 0.16F, R * (0.9F + rnd.nextFloat() * 0.9F), L, FxEntity.RISE | FxEntity.FLICKER);
                }
                for (int k = 0; k < 8; k++) {
                    Vec3 o = AbilityUtil.randomInDisc(rnd, g, R * 0.9).add(0, 0.5 + rnd.nextFloat() * R * 2, 0);
                    make(level, FxEntity.SPHERE, o, UP, 0xFFD24A, 0.1F + rnd.nextFloat() * 0.12F, 0, L,
                            FxEntity.PULSE | FxEntity.FLICKER);
                }
            }
            case WATER -> {
                // aneis brancos de ondulacao no chao + pilar ciano
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.RING, g.add(0, 0.1, 0), UP, WHITE, R * (0.4F + 0.25F * k), 0.05F, L, FxEntity.EXPAND);
                }
                make(level, FxEntity.PILLAR, g, UP, 0x38D6FF, R * 0.3F, R * 2.6F, L, FxEntity.RISE);
                make(level, FxEntity.DOME, g, UP, 0x38D6FF, R * 0.7F, 0, L, FxEntity.PULSE);
            }
            case LIGHTNING -> {
                // raios azul-brancos e tempestade
                for (int k = 0; k < 5; k++) {
                    make(level, FxEntity.PILLAR, AbilityUtil.randomInDisc(rnd, g, R * 0.7), UP, 0xDDF0FF,
                            0.13F, 9.0F, Math.min(L, 20), FxEntity.FLICKER);
                }
                make(level, FxEntity.SPHERE, g.add(0, 1.4, 0), UP, 0x7FB4FF, R * 0.3F, 0, L, FxEntity.PULSE | FxEntity.FLICKER);
                make(level, FxEntity.DOME, g, UP, 0x7FB4FF, R * 0.9F, 0, Math.min(L, 24), FxEntity.FLICKER);
            }
            case EARTH -> {
                // monolitos de arenito + pedras escuras flutuando
                make(level, FxEntity.BOX, g, UP, 0xC9A66B, R * 0.22F, R * 2.2F, L, FxEntity.RISE | FxEntity.SOLID);
                for (int k = 0; k < 8; k++) {
                    make(level, FxEntity.BOX, off(g, R * (0.5 + rnd.nextFloat() * 0.4), k * 45 + rnd.nextInt(15), 0),
                            UP, 0xB8955A, R * (0.12F + rnd.nextFloat() * 0.08F), R * (1.0F + rnd.nextFloat() * 1.2F), L,
                            FxEntity.RISE | FxEntity.SOLID);
                }
                for (int k = 0; k < 6; k++) {
                    Vec3 o = AbilityUtil.randomInDisc(rnd, g, R * 0.8).add(0, R * (0.4 + rnd.nextFloat() * 1.2), 0);
                    make(level, FxEntity.ROCK, o, UP, 0x4A4458, R * (0.1F + rnd.nextFloat() * 0.08F), 0, L, FxEntity.SOLID);
                }
            }
            case WIND -> {
                // cupula branca grande com aneis (halo)
                make(level, FxEntity.DOME, g, UP, 0xF0FFFF, R * 1.0F, 0, L, FxEntity.PULSE);
                make(level, FxEntity.RING, g.add(0, R * 0.95, 0), UP, WHITE, R * 0.9F, 0.05F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, g.add(0, R * 0.5, 0), UP, WHITE, R * 1.05F, 0.04F, L, FxEntity.SPIN);
                make(level, FxEntity.FUNNEL, g, UP, 0xF0FFFF, R * 0.7F, R * 2.0F, L, FxEntity.SPIN | FxEntity.PULSE);
            }
            case LIGHT -> {
                // pilar enorme e grosso amarelo-claro + coroa de espinhos brancos no chao
                make(level, FxEntity.BEAM, g, UP, 0xFFFFC8, R * 0.45F, 30.0F, L, FxEntity.FLICKER);
                make(level, FxEntity.BEAM, g, UP, WHITE, R * 0.22F, 32.0F, L, 0);
                for (int k = 0; k < 10; k++) {
                    double a = Math.toRadians(k * 36);
                    make(level, FxEntity.SPIKE, off(g, R * 0.95, k * 36, 0), new Vec3(Math.cos(a) * 0.7, 1, Math.sin(a) * 0.7),
                            WHITE, R * 0.09F, R * 0.55F, L, FxEntity.RISE);
                }
                make(level, FxEntity.RING, g.add(0, 0.1, 0), UP, WHITE, R * 0.95F, 0.06F, L, 0);
            }
            case DARKNESS -> {
                make(level, FxEntity.DOME, g, UP, c, R * 0.85F, 0, L, FxEntity.PULSE | FxEntity.SPIN);
                make(level, FxEntity.SPHERE, g.add(0, R * 0.45, 0), UP, c, R * 0.22F, 0, L, FxEntity.PULSE);
            }
            case METAL -> {
                // blocos/cubos cinza de metal espalhados
                for (int k = 0; k < 10; k++) {
                    Vec3 o = AbilityUtil.randomInDisc(rnd, g, R * 0.85).add(0, rnd.nextFloat() * R * 0.8, 0);
                    float half = 0.3F + rnd.nextFloat() * 0.3F;
                    make(level, FxEntity.BOX, o, rndDir(rnd, 1), 0x9AA1AB, half, half * 2, L, FxEntity.SOLID);
                }
            }
            case ECLIPSE -> {
                // raio branco fino do chao ate um disco escuro no ceu, cupula de vidro e aneis no chao
                Vec3 beam = new Vec3(0.25, 1, 0.1).normalize();
                make(level, FxEntity.BEAM, g, beam, WHITE, 0.16F, R * 3.2F, L, FxEntity.FLICKER);
                Vec3 top = g.add(beam.scale(R * 3.2));
                make(level, FxEntity.SPHERE, top, UP, 0x0A0F1A, R * 0.45F, 0, L, FxEntity.SOLID | FxEntity.PULSE);
                make(level, FxEntity.RING, top, new Vec3(1, 0, 0), WHITE, R * 0.47F, 0.05F, L, 0);
                make(level, FxEntity.RING, top, new Vec3(0, 1, 0), WHITE, R * 0.47F, 0.05F, L, 0);
                make(level, FxEntity.RING, top, new Vec3(0, 0, 1), WHITE, R * 0.47F, 0.05F, L, 0);
                make(level, FxEntity.DOME, g, UP, WHITE, R * 1.0F, 0, L, FxEntity.PULSE);
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.RING, g.add(0, 0.1, 0), UP, WHITE, R * (0.35F + 0.28F * k), 0.05F, L, FxEntity.EXPAND);
                }
            }
            case BLOOD -> {
                // poca rosada/vermelha brilhante com bolha e filetes vermelhos
                make(level, FxEntity.DISC, g.add(0, 0.05, 0), UP, 0xE8405C, R * 0.9F, 0.0F, L, FxEntity.PULSE);
                make(level, FxEntity.DOME, g, UP, 0xE8405C, R * 0.35F, 0, L, FxEntity.PULSE);
                for (int k = 0; k < 6; k++) {
                    double a = Math.toRadians(rnd.nextInt(360));
                    make(level, FxEntity.BEAM, g.add(0, 0.2, 0), new Vec3(Math.cos(a), 0.5, Math.sin(a)), 0xB01028,
                            0.05F, R * (1.2F + rnd.nextFloat()), L, FxEntity.FLICKER);
                }
            }
            case CELESTIAL -> {
                // grande sol pessego com aneis brancos finos e raios rosados
                make(level, FxEntity.SPHERE, mid, UP, 0xFFC090, R * 0.55F, 0, L, FxEntity.PULSE);
                make(level, FxEntity.RING, mid.add(0, R * 0.65, 0), UP, WHITE, R * 0.45F, 0.04F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, mid, UP, WHITE, R * 0.95F, 0.035F, L, FxEntity.SPIN);
                make(level, FxEntity.DOME, g, UP, 0xFFC090, R * 1.05F, 0, L, FxEntity.PULSE);
                for (int k = 0; k < 3; k++) {
                    make(level, FxEntity.BEAM, mid, rndDir(rnd, 0.6), 0xFF7A8A, 0.07F, R * 1.3F, L, FxEntity.FLICKER);
                }
            }
            case GRAVITY -> {
                // portal roxo em pe, aneis brancos horizontais e pedras claras flutuando
                Vec3 cc = g.add(0, R * 0.9, 0);
                make(level, FxEntity.DISC, cc, new Vec3(0, 0, 1), 0xD060FF, R * 0.45F, 0.0F, L, FxEntity.PULSE);
                make(level, FxEntity.RING, cc, new Vec3(0, 0, 1), 0xD060FF, R * 0.47F, 0.05F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, cc.add(0, R * 0.4, 0), UP, WHITE, R * 0.8F, 0.03F, L, FxEntity.SPIN);
                make(level, FxEntity.RING, cc.add(0, R * 0.15, 0), UP, WHITE, R * 1.1F, 0.03F, L, FxEntity.SPIN);
                for (int k = 0; k < 7; k++) {
                    Vec3 o = AbilityUtil.randomInDisc(rnd, g, R * 0.9).add(0, 0.1 + rnd.nextFloat() * R * 1.2, 0);
                    make(level, FxEntity.ROCK, o, UP, 0xD8D2B0, R * (0.1F + rnd.nextFloat() * 0.1F), 0, L, FxEntity.SOLID);
                }
            }
            case TIME -> {
                // relogios brancos em pe com numeros romanos (dois planos cruzados)
                Vec3 cc = g.add(0, R * 0.95, 0);
                float yaw = rnd.nextInt(360);
                MagicCircleEntity.spawnVertical(level, cc, el, WHITE, R * 0.85F, L, yaw);
                MagicCircleEntity.spawnVertical(level, cc, el, WHITE, R * 0.85F, L, yaw + 90.0F);
                for (int k = 0; k < 6; k++) {
                    Vec3 o = AbilityUtil.randomInDisc(rnd, cc, R * 0.9).add(0, (rnd.nextFloat() - 0.5) * R, 0);
                    make(level, FxEntity.SPHERE, o, UP, 0xC8FF8C, 0.07F, 0, L, FxEntity.PULSE | FxEntity.FLICKER);
                }
            }
            case REALITY_COLLAPSE -> {
                // sol negro: esfera escura com aro branco brilhante (3 aneis cruzados)
                Vec3 cc = g.add(0, R * 1.1, 0);
                make(level, FxEntity.SPHERE, cc, UP, 0x070A14, R * 0.42F, 0, L, FxEntity.SOLID | FxEntity.PULSE);
                make(level, FxEntity.RING, cc, new Vec3(1, 0, 0), WHITE, R * 0.45F, 0.05F, L, 0);
                make(level, FxEntity.RING, cc, new Vec3(0, 1, 0), WHITE, R * 0.45F, 0.05F, L, 0);
                make(level, FxEntity.RING, cc, new Vec3(0, 0, 1), WHITE, R * 0.45F, 0.05F, L, 0);
            }
            default -> {
                family(level, g, el, R, L, rnd);
                extras(level, g, el, R, L, rnd);
            }
        }
    }
}
