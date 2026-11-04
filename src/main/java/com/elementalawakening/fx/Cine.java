package com.elementalawakening.fx;

import com.elementalawakening.ability.AbilityUtil;
import com.elementalawakening.ability.Scheduler;
import com.elementalawakening.element.Element;
import com.elementalawakening.entity.FxEntity;
import com.elementalawakening.entity.MagicCircleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Sequencias cinematograficas: CARGA (o simbolo se contrai) -> EXPLOSAO (tremor de tela, clarao, estrutura brota
 * com "pop") -> SUSTENTACAO (pecas orbitam, flutuam, caem) -> DISSIPACAO. Cada elemento tem a sua coreografia.
 * Tudo e montado de uma vez, com atrasos por peca (o cliente anima sozinho, sem custo por tick).
 */
public final class Cine {
    private static final int SPH = FxEntity.SPHERE, PIL = FxEntity.PILLAR, SPK = FxEntity.SPIKE, BEM = FxEntity.BEAM,
            RNG = FxEntity.RING, DOM = FxEntity.DOME, FUN = FxEntity.FUNNEL, DSC = FxEntity.DISC,
            BOX = FxEntity.BOX, ROK = FxEntity.ROCK;
    private static final int EXP = FxEntity.EXPAND, RIS = FxEntity.RISE, PUL = FxEntity.PULSE, SPN = FxEntity.SPIN,
            FLK = FxEntity.FLICKER, SOL = FxEntity.SOLID;
    private static final int POP = FxEntity.POP, SLOW = FxEntity.SLOW, SHRINK = FxEntity.SHRINK, BELL = FxEntity.BELL;
    private static final int WHITE = 0xFFFFFF;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int REL = 6; // tick da explosao (depois da carga)

    private final ServerLevel level;
    private final Vec3 g;
    private final Element el;
    private final float size;
    private final float R;
    private final int L;
    private final int c;
    private final RandomSource rnd;
    private final Vec3 mid;

    private Cine(ServerLevel level, Vec3 g, Element el, float size, int life) {
        this.level = level;
        this.g = g;
        this.el = el;
        this.size = size;
        this.R = size * 0.85F;
        this.L = Math.max(24, life);
        this.c = el.color;
        this.rnd = level.getRandom();
        this.mid = g.add(0, R * 0.9, 0);
    }

    /** Toca a sequencia do elemento em g (no chao). size = tamanho do circulo; life em ticks. */
    public static void play(ServerLevel level, Vec3 g, Element el, float size, int life) {
        new Cine(level, g, el, size, life).sequence();
    }

    // ------------------------------------------------------------------ ferramentas
    private FxEntity.Spec s(int shape, Vec3 pos, int color) {
        return new FxEntity.Spec(shape).pos(pos).color(color).life(L).delay(REL);
    }

    private static int lighten(int color) {
        int r = (color >> 16) & 255, gg = (color >> 8) & 255, b = color & 255;
        return ((r + (255 - r) / 2) << 16) | ((gg + (255 - gg) / 2) << 8) | (b + (255 - b) / 2);
    }

    private void column(int color, float r, float h, int style) {
        s(PIL, g, color).r(r).h(h).style(style | RIS).grow(POP).spawn(level);
    }

    private void beam(int color, float r, float h, Vec3 dir, int style) {
        s(BEM, g, color).dir(dir).r(r).h(h).style(style | RIS).grow(POP).spawn(level);
    }

    private void rings(int n, int color, float tube) {
        for (int i = 0; i < n; i++) {
            new FxEntity.Spec(RNG).pos(g.add(0, 0.12, 0)).color(i % 2 == 0 ? color : WHITE).r(R * (0.5F + 0.3F * i)).h(tube)
                    .life(16).delay(REL + i * 3).style(EXP).spawn(level);
        }
    }

    private void dome(int color, float rf, int style) {
        s(DOM, g, color).r(R * rf).style(style).grow(POP).spawn(level);
    }

    private void pool(int color, float rf) {
        s(DSC, g.add(0, 0.05, 0), color).r(R * rf).h(0.0F).style(PUL).grow(POP).spawn(level);
    }

    private void core(int color, float rf, double yFactor, int style, boolean solid) {
        s(SPH, g.add(0, R * yFactor, 0), color).r(R * rf).style(style | (solid ? SOL : 0)).grow(POP).spawn(level);
    }

    private void vring(int color, float rf, Vec3 dir, double yFactor, float tube) {
        s(RNG, g.add(0, R * yFactor, 0), color).dir(dir).r(R * rf).h(tube).style(SPN).grow(POP).spawn(level);
    }

    /** Detritos que explodem para fora em arco e caem. */
    private void debris(int n, int shape, int color, float sz, boolean solid) {
        for (int k = 0; k < n; k++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double sp = 0.14 + rnd.nextDouble() * 0.16;
            s(shape, g.add(0, 0.4, 0), color).r(sz).h(shape == BOX ? sz * 2.2F : 0.0F).life(26)
                    .vel(Math.cos(a) * sp, 0.26 + rnd.nextDouble() * 0.2, Math.sin(a) * sp).acc(-0.035F)
                    .grow(SHRINK).style(solid ? SOL : 0).spawn(level);
        }
    }

    /** Pecas que flutuam devagar (vy>0 sobe, vy<0 cai): brasas, neve, bolhas... */
    private void drift(int n, int color, float vy, float sz, float spread) {
        for (int k = 0; k < n; k++) {
            Vec3 o = AbilityUtil.randomInDisc(rnd, g, R * spread).add(0, 0.3 + rnd.nextDouble() * R * 1.6, 0);
            s(SPH, o, color).r(sz * (0.7F + rnd.nextFloat() * 0.6F)).life(Math.min(L, 46)).delay(REL + rnd.nextInt(10))
                    .vel((rnd.nextDouble() - 0.5) * 0.03, vy, (rnd.nextDouble() - 0.5) * 0.03).grow(BELL)
                    .style(FLK | PUL).spawn(level);
        }
    }

    /** Pecas que giram em volta do centro (w > 0 horario, w < 0 anti-horario). */
    private void orbiters(int n, int shape, int color, float orbitR, float height, float w, float sz, boolean solid) {
        for (int k = 0; k < n; k++) {
            s(shape, g.add(0, height, 0), color).r(sz).h(shape == BOX ? sz * 2.0F : 0.0F)
                    .orbit(orbitR, w, 360.0F * k / n).style(PUL | (solid ? SOL : 0)).grow(POP).spawn(level);
        }
    }

    /** Pecas que se juntam ao centro (colapso). */
    private void converge(int n, int shape, int color, float sz, boolean solid) {
        for (int k = 0; k < n; k++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = R * (0.8 + rnd.nextDouble() * 0.5);
            double y = R * (0.3 + rnd.nextDouble() * 1.2);
            int t = 14;
            s(shape, g.add(Math.cos(a) * d, y, Math.sin(a) * d), color).r(sz).h(shape == BOX ? sz * 2.0F : 0.0F)
                    .vel(-Math.cos(a) * d / t, (R * 0.9 - y) / t, -Math.sin(a) * d / t).life(t).delay(REL + 2)
                    .grow(SHRINK).style(solid ? SOL : 0).spawn(level);
        }
    }

    private void bolts(int n, int color, float h) {
        for (int k = 0; k < n; k++) {
            s(PIL, AbilityUtil.randomInDisc(rnd, g, R * 0.8), color).r(0.1F).h(h).life(7).delay(REL + k * 2 + rnd.nextInt(3))
                    .style(FLK).grow(POP).spawn(level);
        }
    }

    private void tendrils(int n, int color, float len) {
        for (int k = 0; k < n; k++) {
            double a = Math.toRadians(k * 360.0 / n + rnd.nextInt(15));
            s(BEM, g.add(0, 0.2, 0), color).dir(new Vec3(Math.cos(a) * 0.7, 1, Math.sin(a) * 0.7)).r(0.07F)
                    .h(len * (0.7F + rnd.nextFloat() * 0.5F)).style(FLK | RIS).grow(POP).delay(REL + k).spawn(level);
        }
    }

    private void spikesRing(int n, int color, float rf, float len) {
        for (int k = 0; k < n; k++) {
            double a = Math.toRadians(k * 360.0 / n);
            Vec3 p = g.add(Math.cos(a) * R * rf, 0, Math.sin(a) * R * rf);
            s(SPK, p, color).dir(new Vec3(Math.cos(a) * 0.8, 1, Math.sin(a) * 0.8)).r(R * 0.1F)
                    .h(len * (0.7F + rnd.nextFloat() * 0.6F)).style(RIS).grow(POP).delay(REL + (k % 4)).spawn(level);
        }
    }

    private void monoliths(int n, int color, float w, float hMin, float hMax) {
        for (int k = 0; k < n; k++) {
            double a = Math.toRadians(k * 360.0 / n + rnd.nextInt(20));
            Vec3 p = g.add(Math.cos(a) * R * (0.4 + rnd.nextDouble() * 0.5), 0, Math.sin(a) * R * (0.4 + rnd.nextDouble() * 0.5));
            s(BOX, p, color).r(w * (0.8F + rnd.nextFloat() * 0.5F)).h(hMin + rnd.nextFloat() * (hMax - hMin))
                    .style(RIS | SOL).grow(POP).delay(REL + k % 5).spawn(level);
        }
    }

    private void rays(int n, int color, float len, double yFactor) {
        for (int k = 0; k < n; k++) {
            double a = Math.toRadians(k * 360.0 / n);
            s(BEM, g.add(0, R * yFactor, 0), color).dir(new Vec3(Math.cos(a), 0, Math.sin(a))).r(0.06F).h(len)
                    .style(FLK).grow(POP).spawn(level);
        }
    }

    private void sfx(SoundEvent sound, float vol, float pitch, int atTick) {
        Scheduler.run(atTick, () -> AbilityUtil.sound(level, g, sound, vol, pitch));
    }

    // ------------------------------------------------------------------ abertura comum
    private void sequence() {
        // 1) CARGA: o aro se contrai para o centro e uma faisca cresce
        AbilityUtil.sound(level, g, SoundEvents.BEACON_ACTIVATE, 1.6F, 0.5F);
        new FxEntity.Spec(RNG).pos(g.add(0, 0.15, 0)).color(c).r(R * 1.1F).h(0.1F).life(REL + 2).style(SPN).grow(SHRINK).spawn(level);
        new FxEntity.Spec(SPH).pos(g.add(0, 0.8, 0)).color(lighten(c)).r(0.5F).life(REL).grow(SLOW).style(PUL).spawn(level);

        // 2) EXPLOSAO: tremor, clarao, barras de cinema nas grandes
        Scheduler.run(REL, () -> {
            AbilityUtil.sound(level, g, SoundEvents.GENERIC_EXPLODE, 1.8F, 0.55F);
            ScreenFx.shake(level, g, 40, 16, 0.5F + R * 0.07F);
            ScreenFx.flash(level, g, 30, 7, 0.4F, lighten(c));
            ScreenFx.zoom(level, g, 30, 14, 0.12F);
            if (size >= 8.0F) ScreenFx.bars(level, g, 40, Math.min(L, 70));
        });

        // 3) coreografia propria do elemento
        switch (el) {
            case FIRE -> {
                column(0xFF4A1A, R * 0.5F, R * 2.8F, FLK);
                column(0xFFC83C, R * 0.28F, R * 2.4F, FLK);
                rings(3, 0xFFB040, 0.1F);
                debris(8, ROK, 0xFF6A10, 0.22F, true);
                drift(16, 0xFFD24A, 0.12F, 0.12F, 0.9F);
                orbiters(5, SPH, 0xFFE070, R * 0.8F, R * 0.5F, 12.0F, 0.16F, false);
            }
            case WATER -> {
                rings(4, WHITE, 0.06F);
                column(0x38D6FF, R * 0.3F, R * 2.6F, 0);
                dome(0x38D6FF, 0.9F, PUL);
                drift(14, 0xBFEFFF, -0.1F, 0.1F, 0.8F);
                orbiters(4, SPH, WHITE, R * 0.7F, R, 10.0F, 0.15F, false);
            }
            case LIGHTNING -> {
                bolts(7, 0xDDF0FF, 14.0F);
                core(0x7FB4FF, 0.3F, 0.5, PUL | FLK, false);
                rings(2, 0xAFD8FF, 0.05F);
                dome(0x7FB4FF, 0.9F, FLK);
                sfx(SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.9F, REL);
            }
            case EARTH -> {
                monoliths(7, 0xB8955A, R * 0.14F, R * 1.2F, R * 2.2F);
                debris(10, BOX, 0xC9A66B, 0.25F, true);
                orbiters(5, ROK, 0x4A4458, R * 0.8F, R * 0.8F, 8.0F, 0.2F, true);
            }
            case WIND -> {
                s(FUN, g, 0xF0FFFF).r(R * 0.85F).h(R * 2.6F).style(SPN | PUL).grow(POP).spawn(level);
                rings(3, WHITE, 0.04F);
                drift(18, WHITE, 0.15F, 0.06F, 1.0F);
                dome(0xF0FFFF, 1.0F, PUL);
            }
            case LIGHT -> {
                beam(0xFFFFC8, R * 0.45F, 30.0F, UP, FLK);
                beam(WHITE, R * 0.22F, 32.0F, UP, 0);
                spikesRing(10, WHITE, 0.95F, R * 0.55F);
                rings(3, WHITE, 0.06F);
                rays(8, 0xFFFFC8, R, 0.9);
            }
            case DARKNESS -> {
                dome(0x9A55D8, 0.9F, PUL | SPN);
                tendrils(8, 0x3A2070, R * 2.0F);
                orbiters(6, SPH, 0x120818, R * 0.7F, R * 0.7F, -9.0F, 0.17F, true);
                drift(12, 0x7A3FB0, 0.08F, 0.1F, 0.8F);
            }
            case METAL -> {
                spikesRing(7, 0xB0B7C3, 0.7F, R * 1.2F);
                debris(10, BOX, 0x9AA1AB, 0.3F, true);
                rings(2, 0xD0D6DE, 0.08F);
                orbiters(4, BOX, 0x9AA1AB, R * 0.7F, R * 0.9F, 6.0F, 0.25F, true);
            }
            case ECLIPSE -> {
                Vec3 bd = new Vec3(0.25, 1, 0.1).normalize();
                beam(WHITE, 0.16F, R * 3.2F, bd, FLK);
                Vec3 top = g.add(bd.scale(R * 3.2));
                s(SPH, top, 0x0A0F1A).r(R * 0.45F).style(SOL | PUL).grow(POP).spawn(level);
                for (Vec3 d : new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)}) {
                    s(RNG, top, WHITE).dir(d).r(R * 0.47F).h(0.05F).grow(POP).spawn(level);
                }
                dome(WHITE, 1.0F, PUL);
                rings(3, WHITE, 0.05F);
            }
            case BLOOD -> {
                pool(0xE8405C, 0.9F);
                dome(0xE8405C, 0.35F, PUL);
                tendrils(8, 0xB01028, R * 1.6F);
                orbiters(6, SPH, 0xE8405C, R * 0.6F, R * 0.6F, 14.0F, 0.16F, false);
                drift(14, 0xB01028, -0.12F, 0.09F, 0.7F);
            }
            case CELESTIAL -> {
                core(0xFFC090, 0.55F, 0.9, PUL, false);
                vring(WHITE, 0.95F, UP, 0.9, 0.035F);
                vring(WHITE, 0.45F, UP, 1.55, 0.04F);
                dome(0xFFC090, 1.05F, PUL);
                for (int k = 0; k < 4; k++) {
                    s(BEM, mid, 0xFF7A8A).dir(new Vec3(rnd.nextFloat() - 0.5, rnd.nextFloat() * 0.6, rnd.nextFloat() - 0.5))
                            .r(0.07F).h(R * 1.3F).style(FLK).grow(POP).spawn(level);
                }
            }
            case GRAVITY -> {
                s(DSC, mid, 0xD060FF).dir(new Vec3(0, 0, 1)).r(R * 0.45F).h(0.0F).style(PUL).grow(POP).spawn(level);
                s(RNG, mid, 0xD060FF).dir(new Vec3(0, 0, 1)).r(R * 0.47F).h(0.05F).style(SPN).grow(POP).spawn(level);
                vring(WHITE, 0.8F, UP, 1.3, 0.03F);
                vring(WHITE, 1.1F, UP, 1.05, 0.03F);
                for (int k = 0; k < 8; k++) {
                    s(ROK, AbilityUtil.randomInDisc(rnd, g, R * 0.9).add(0, 0.2, 0), 0xD8D2B0).r(0.18F + rnd.nextFloat() * 0.18F)
                            .vel(0, 0.04 + rnd.nextDouble() * 0.08, 0).grow(BELL).style(SOL).spawn(level);
                }
            }
            case TIME -> {
                Scheduler.run(REL, () -> {
                    float yaw = rnd.nextInt(360);
                    MagicCircleEntity.spawnVertical(level, mid.add(0, R * 0.05, 0), el, WHITE, R * 0.85F, L, yaw);
                    MagicCircleEntity.spawnVertical(level, mid.add(0, R * 0.05, 0), el, WHITE, R * 0.85F, L, yaw + 90.0F);
                });
                orbiters(8, SPH, 0xC8FF8C, R * 0.9F, R * 0.95F, 18.0F, 0.08F, false);
                drift(10, 0xC8FF8C, 0.1F, 0.07F, 0.9F);
                rings(2, WHITE, 0.04F);
            }
            case REALITY_COLLAPSE -> {
                converge(10, ROK, WHITE, 0.2F, true);
                s(SPH, mid.add(0, R * 0.2, 0), 0x070A14).r(R * 0.42F).delay(REL + 12).style(SOL | PUL).grow(POP).spawn(level);
                for (Vec3 d : new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)}) {
                    s(RNG, mid.add(0, R * 0.2, 0), WHITE).dir(d).r(R * 0.45F).h(0.05F).delay(REL + 12).style(SPN).grow(POP).spawn(level);
                }
            }
            // ------------------------------------------------ elementos novos
            case ICE -> {
                spikesRing(10, 0xA8E8FF, 0.6F, R * 1.4F);
                s(SPK, g, 0xFFFFFF).r(R * 0.2F).h(R * 2.0F).style(RIS).grow(POP).spawn(level);
                drift(18, WHITE, -0.1F, 0.06F, 1.1F);
                dome(0xE8FFFF, 0.7F, PUL);
                rings(2, WHITE, 0.05F);
            }
            case NATURE -> {
                tendrils(9, 0x5EDB6A, R * 2.0F);
                for (int k = 0; k < 5; k++) {
                    double a = Math.toRadians(k * 72);
                    s(SPH, g.add(Math.cos(a) * R * 0.5, R * 1.6, Math.sin(a) * R * 0.5), 0xFF9AD0).r(0.24F).delay(REL + 8).style(PUL).grow(POP).spawn(level);
                }
                pool(0x5EDB6A, 0.9F);
                drift(14, 0xB8FF8C, 0.06F, 0.08F, 1.0F);
            }
            case POISON -> {
                dome(0x9BE03A, 1.0F, PUL);
                pool(0x6AA020, 0.9F);
                for (int k = 0; k < 10; k++) {
                    s(SPH, AbilityUtil.randomInDisc(rnd, g, R * 0.7).add(0, 0.3, 0), 0x9BE03A).r(0.25F + rnd.nextFloat() * 0.3F)
                            .vel(0, 0.05 + rnd.nextDouble() * 0.05, 0).life(34).delay(REL + k * 2).grow(BELL).style(PUL).spawn(level);
                }
                rings(2, 0xC8FF70, 0.06F);
            }
            case SAND -> {
                s(FUN, g, 0xE6C27A).r(R * 0.9F).h(R * 2.2F).style(SPN | PUL).grow(POP).spawn(level);
                debris(10, BOX, 0xE6C27A, 0.18F, true);
                rings(3, 0xFFE9B0, 0.1F);
                drift(16, 0xE6C27A, 0.1F, 0.08F, 1.1F);
            }
            case STEAM -> {
                for (int k = 0; k < 6; k++) {
                    s(SPH, AbilityUtil.randomInDisc(rnd, g, R * 0.6).add(0, 0.5, 0), WHITE).r(R * 0.3F).vel(0, 0.06, 0)
                            .life(40).delay(REL + k * 3).grow(BELL).style(PUL).spawn(level);
                }
                dome(0xDDE8EE, 1.0F, PUL);
                rings(2, WHITE, 0.08F);
            }
            case MAGMA -> {
                pool(0xFF3A10, 0.9F);
                column(0xFF6B1A, R * 0.35F, R * 2.2F, FLK);
                debris(9, ROK, 0x802010, 0.25F, true);
                rings(2, 0xFFC040, 0.1F);
                drift(12, 0xFFA030, 0.1F, 0.1F, 0.8F);
            }
            case STORM -> {
                s(FUN, g, 0x6C8CFF).r(R * 0.9F).h(R * 2.4F).style(SPN | PUL).grow(POP).spawn(level);
                bolts(5, 0xEAF4FF, 16.0F);
                rings(2, 0xDDF0FF, 0.06F);
                dome(0x6C8CFF, 1.0F, FLK);
                sfx(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5F, 1.1F, REL);
            }
            case CRYSTAL -> {
                spikesRing(8, 0xB48CFF, 0.7F, R * 1.6F);
                s(SPK, g, 0xE8D8FF).r(R * 0.2F).h(R * 1.9F).style(RIS).grow(POP).spawn(level);
                orbiters(5, SPH, 0xE8D8FF, R * 0.7F, R * 0.9F, 10.0F, 0.14F, false);
                dome(0xE8D8FF, 0.7F, PUL);
            }
            case SOUND -> {
                for (int k = 0; k < 5; k++) {
                    s(RNG, g.add(0, R * (0.3 + 0.3 * k), 0), k % 2 == 0 ? c : WHITE).r(R * (0.7F - 0.07F * k)).h(0.05F)
                            .delay(REL + k * 2).life(22).style(EXP | SPN).spawn(level);
                }
                rings(4, c, 0.05F);
                dome(c, 0.9F, PUL);
            }
            case SHADOW -> {
                dome(0x7A56C8, 1.0F, PUL | SPN);
                tendrils(7, 0x3A2070, R * 1.8F);
                s(SPH, mid, 0x120818).r(R * 0.25F).style(SOL | PUL).grow(POP).spawn(level);
                vring(0x9A70E0, 0.32F, new Vec3(0.3, 1, 0.2), 0.9, 0.04F);
                drift(12, 0x7A56C8, 0.07F, 0.1F, 0.9F);
            }
            case ACID -> {
                pool(0xC8FF2E, 0.9F);
                dome(0x88C020, 0.9F, PUL);
                for (int k = 0; k < 6; k++) {
                    s(PIL, AbilityUtil.randomInDisc(rnd, g, R * 0.7).add(0, R, 0), 0xE8FF70).dir(new Vec3(0, -1, 0)).r(0.05F).h(R * 0.8F)
                            .delay(REL + k * 3).style(FLK).grow(POP).spawn(level);
                }
                drift(12, 0xC8FF2E, 0.08F, 0.2F, 0.7F);
            }
            case GLASS -> {
                debris(10, BOX, 0xBFF4FF, 0.12F, true);
                spikesRing(8, WHITE, 0.6F, R * 1.2F);
                rings(3, 0xBFF4FF, 0.05F);
                orbiters(4, SPH, WHITE, R * 0.7F, R * 0.9F, 8.0F, 0.1F, false);
            }
            case SAKURA -> {
                orbiters(8, DSC, 0xFFA6C9, R * 0.8F, R * 0.8F, 6.0F, 0.22F, false);
                drift(16, WHITE, -0.05F, 0.1F, 1.0F);
                pool(0xFFA6C9, 0.9F);
                rings(2, WHITE, 0.04F);
            }
            case BONE -> {
                monoliths(7, 0xEDE6D0, R * 0.08F, R * 1.2F, R * 2.0F);
                s(ROK, g.add(0, R * 2.1, 0), 0xEDE6D0).r(R * 0.2F).delay(REL + 6).style(SOL).grow(POP).spawn(level);
                debris(8, BOX, 0xEDE6D0, 0.12F, true);
                rings(2, 0xBFB59A, 0.06F);
            }
            case SPIRIT -> {
                for (int k = 0; k < 5; k++) {
                    double a = Math.toRadians(k * 72);
                    s(SPK, g.add(Math.cos(a) * R * 0.5, 0, Math.sin(a) * R * 0.5), 0x8CFFE0).r(0.18F).h(R * 1.6F)
                            .vel(0, 0.02, 0).style(RIS | FLK).grow(POP).delay(REL + k * 2).spawn(level);
                }
                orbiters(6, SPH, 0x8CFFE0, R * 0.8F, R * 0.8F, 16.0F, 0.15F, false);
                drift(14, WHITE, 0.09F, 0.08F, 0.9F);
                rings(2, WHITE, 0.05F);
            }
            case PLASMA -> {
                core(0xFF4DFF, 0.35F, 0.9, PUL | FLK, false);
                rays(10, WHITE, R * 0.95F, 0.9);
                vring(0xFF4DFF, 0.6F, new Vec3(1, 0, 0), 0.9, 0.05F);
                vring(WHITE, 0.6F, new Vec3(0, 0, 1), 0.9, 0.05F);
                bolts(5, WHITE, 8.0F);
            }
            case DREAM -> {
                vring(WHITE, 0.55F, new Vec3(0.2, 1, 0.5), 1.2, 0.05F);
                orbiters(8, SPH, WHITE, R * 0.9F, R * 0.9F, 5.0F, 0.08F, false);
                drift(20, 0xFFB6F0, 0.06F, 0.08F, 1.0F);
                dome(0xFFB6F0, 0.9F, PUL);
            }
            case NIGHTMARE -> {
                for (int k = 0; k < 6; k++) {
                    double a = Math.toRadians(k * 60);
                    s(SPK, g.add(Math.cos(a) * R * 0.5, R * 2.0, Math.sin(a) * R * 0.5), 0xC0306A).dir(new Vec3(0, -1, 0)).r(0.15F)
                            .h(R * 0.95F).style(RIS | FLK).grow(POP).delay(REL + k * 2).spawn(level);
                }
                dome(0x400020, 1.0F, FLK);
                tendrils(6, 0xC0306A, R * 1.6F);
                orbiters(5, SPH, 0x1A0510, R * 0.7F, R * 0.8F, -10.0F, 0.17F, true);
            }
            case MIRROR -> {
                s(DSC, mid, WHITE).dir(new Vec3(0, 0, 1)).r(R * 0.55F).h(0.0F).style(PUL).grow(POP).spawn(level);
                s(DSC, mid, c).dir(new Vec3(1, 0, 0)).r(R * 0.55F).h(0.0F).style(PUL).grow(POP).spawn(level);
                s(DSC, mid, WHITE).dir(new Vec3(1, 0, 1)).r(R * 0.45F).h(0.0F).style(PUL).grow(POP).spawn(level);
                debris(8, BOX, 0xE0F0FF, 0.08F, true);
                rings(2, WHITE, 0.04F);
            }
            case RUNE -> {
                monoliths(4, 0xFFD060, R * 0.14F, R * 0.9F, R * 1.2F);
                rings(3, 0xFFD060, 0.06F);
                orbiters(8, SPH, 0xFFD060, R * 0.9F, R * 0.9F, -7.0F, 0.1F, false);
                vring(WHITE, 0.6F, new Vec3(1, 0, 1), 1.0, 0.04F);
            }
            case COMET -> {
                s(SPH, g.add(0, R * 2.4, 0), WHITE).r(R * 0.25F).vel(0.0, -0.12, 0.0).life(20).style(PUL).grow(POP).spawn(level);
                s(BEM, g.add(0, R * 2.4, 0), 0x80D8FF).dir(new Vec3(0.3, -1, 0.1)).r(0.12F).h(R * 2.0F).life(20).style(FLK).grow(POP).spawn(level);
                debris(10, ROK, 0x80D8FF, 0.2F, true);
                rings(3, WHITE, 0.07F);
                drift(16, 0x80D8FF, -0.15F, 0.08F, 1.0F);
            }
            case COSMOS -> {
                for (int k = 0; k < 20; k++) {
                    s(SPH, AbilityUtil.randomInDisc(rnd, g, R * 1.3).add(0, R * (0.3 + rnd.nextFloat() * 1.8), 0), WHITE)
                            .r(0.06F + rnd.nextFloat() * 0.05F).style(FLK).grow(BELL).spawn(level);
                }
                core(0x6A5CFF, 0.3F, 0.9, PUL, false);
                vring(0x6A5CFF, 1.0F, new Vec3(0.4, 1, 0.1), 0.9, 0.04F);
                orbiters(3, SPH, 0xB0A8FF, R * 1.0F, R * 0.9F, 5.0F, 0.3F, false);
            }
            case CHAOS -> {
                debris(12, ROK, 0xE03030, 0.28F, true);
                for (int k = 0; k < 8; k++) {
                    s(SPK, g, 0xFF5050).dir(new Vec3(rnd.nextFloat() - 0.5, 1, rnd.nextFloat() - 0.5)).r(0.18F)
                            .h(R * (0.8F + rnd.nextFloat())).style(RIS).grow(POP).delay(REL + k).spawn(level);
                }
                rings(3, 0xFFA050, 0.08F);
                bolts(4, 0xFF5050, 10.0F);
            }
            case ORDER -> {
                monoliths(4, 0xFFD84A, R * 0.14F, R * 1.8F, R * 1.8F);
                beam(0xFFE070, R * 0.2F, 22.0F, UP, 0);
                rings(3, WHITE, 0.05F);
                orbiters(4, BOX, 0xFFD84A, R * 0.9F, R * 1.0F, 6.0F, 0.2F, true);
            }
            case LIFE -> {
                column(0x8A5A2B, R * 0.12F, R * 1.4F, 0);
                s(SPH, g.add(0, R * 1.6, 0), 0x7CFF9A).r(R * 0.5F).delay(REL + 6).style(PUL).grow(POP).spawn(level);
                pool(0x7CFF9A, 0.9F);
                drift(18, 0x7CFF9A, 0.08F, 0.09F, 1.0F);
                rings(3, WHITE, 0.05F);
            }
            case DEATH -> {
                dome(0x606080, 1.1F, FLK);
                for (int k = 0; k < 6; k++) {
                    double a = Math.toRadians(k * 60);
                    s(SPK, g.add(Math.cos(a) * R * 0.5, 0, Math.sin(a) * R * 0.5), 0xB8B8D8).r(0.14F).h(R * 1.3F).style(RIS).grow(POP).delay(REL + k).spawn(level);
                }
                drift(14, 0xB8B8D8, 0.1F, 0.08F, 0.9F);
                orbiters(5, ROK, 0x303040, R * 0.8F, R * 0.8F, -8.0F, 0.22F, true);
            }
            case DRAGON -> {
                column(0xFF8A3C, R * 0.35F, R * 2.2F, FLK);
                spikesRing(6, 0xFF8A3C, 0.6F, R * 1.6F);
                rings(3, 0xFFD060, 0.1F);
                drift(14, 0xFFD060, 0.12F, 0.1F, 0.9F);
                sfx(SoundEvents.GENERIC_EXPLODE, 1.2F, 0.4F, REL + 4);
            }
            case VOID -> {
                converge(10, ROK, 0xA070FF, 0.2F, true);
                s(SPH, mid, 0x07040E).r(R * 0.38F).delay(REL + 12).style(SOL | PUL).grow(POP).spawn(level);
                s(DSC, mid, 0xA070FF).r(R * 0.95F).h(R * 0.4F).delay(REL + 12).style(SPN).grow(POP).spawn(level);
                dome(0x401080, 0.9F, PUL);
            }
            case CREATION -> {
                beam(0xFFF2A0, R * 0.12F, 24.0F, UP, FLK);
                for (int k = 0; k < 8; k++) {
                    s(SPH, g.add(0, 0.5 + k * R * 0.28, 0), WHITE).r(0.14F).orbit(R * 0.6F, 14.0F, k * 45.0F).delay(REL + k * 2)
                            .style(PUL).grow(POP).spawn(level);
                }
                rings(4, 0xFFF2A0, 0.05F);
                core(WHITE, 0.2F, 0.9, PUL, false);
            }
            case ETERNITY -> {
                s(RNG, mid.add(R * 0.35, 0, 0), c).dir(new Vec3(0, 0, 1)).r(R * 0.35F).h(0.05F).style(SPN).grow(POP).spawn(level);
                s(RNG, mid.add(-R * 0.35, 0, 0), WHITE).dir(new Vec3(0, 0, 1)).r(R * 0.35F).h(0.05F).style(SPN).grow(POP).spawn(level);
                vring(c, 0.9F, new Vec3(0.3, 1, 0), 0.9, 0.04F);
                orbiters(6, SPH, WHITE, R * 0.9F, R * 0.9F, 8.0F, 0.1F, false);
                drift(16, 0xA0FFF0, 0.05F, 0.07F, 1.0F);
            }
            case SOLAR -> {
                core(0xFFC93C, 0.5F, 2.2, PUL | FLK, false);
                rays(14, 0xFFE08A, R * 1.1F, 2.2);
                vring(WHITE, 0.7F, new Vec3(0, 0, 1), 2.2, 0.05F);
                vring(0xFFC93C, 0.9F, new Vec3(1, 0, 0), 2.2, 0.04F);
                column(0xFFC93C, R * 0.2F, R * 2.0F, FLK);
                rings(3, 0xFFE08A, 0.08F);
                drift(16, 0xFFD24A, 0.1F, 0.1F, 0.9F);
            }
            case LUNAR -> {
                core(0xBFD0FF, 0.35F, 1.7, PUL, false);
                vring(WHITE, 0.55F, new Vec3(0.2, 1, 0.5), 1.7, 0.05F);
                orbiters(8, SPH, WHITE, R * 0.9F, R * 0.9F, 4.0F, 0.09F, false);
                drift(20, 0xBFD0FF, 0.05F, 0.07F, 1.0F);
                dome(0xBFD0FF, 0.9F, PUL);
                rings(2, WHITE, 0.04F);
            }
            case GOLD -> {
                spikesRing(8, 0xFFD54A, 0.6F, R * 1.5F);
                monoliths(5, 0xFFD54A, R * 0.1F, R * 0.8F, R * 1.4F);
                debris(10, BOX, 0xFFD54A, 0.12F, true);
                rings(3, 0xFFF2B0, 0.07F);
                orbiters(6, SPH, 0xFFF2B0, R * 0.8F, R * 0.8F, 9.0F, 0.12F, false);
            }
            case WEB -> {
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(k * 45);
                    s(BEM, g.add(0, 0.15, 0), 0xE8E8F0).dir(new Vec3(Math.cos(a), 0, Math.sin(a))).r(0.04F).h(R)
                            .style(FLK).grow(POP).delay(REL + k % 3).spawn(level);
                }
                rings(4, 0xE8E8F0, 0.03F);
                tendrils(6, 0xFFFFFF, R * 1.4F);
                pool(0xE8E8F0, 0.9F);
                drift(14, WHITE, -0.05F, 0.05F, 1.0F);
            }
            case RUST -> {
                debris(12, ROK, 0xB5651D, 0.25F, true);
                spikesRing(6, 0x7A3F10, 0.7F, R * 1.1F);
                pool(0x7A3F10, 0.9F);
                rings(2, 0xD08A40, 0.08F);
                drift(14, 0xD08A40, 0.06F, 0.09F, 0.9F);
            }
            case ECHO -> {
                for (int k = 0; k < 6; k++) {
                    s(RNG, g.add(0, 0.2 + k * R * 0.28, 0), k % 2 == 0 ? c : WHITE).r(R * 0.6F).h(0.05F)
                            .delay(REL + k * 2).life(24).style(EXP).spawn(level);
                }
                rings(5, c, 0.04F);
                dome(c, 0.9F, PUL);
                orbiters(4, RNG, WHITE, R * 0.7F, R * 0.9F, 8.0F, 0.3F, false);
            }
            default -> {
                column(c, R * 0.4F, R * 2.4F, FLK);
                rings(3, WHITE, 0.06F);
            }
        }
    }
}
