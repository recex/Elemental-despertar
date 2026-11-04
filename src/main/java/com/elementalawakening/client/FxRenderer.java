package com.elementalawakening.client;

import com.elementalawakening.ElementalMod;
import com.elementalawakening.entity.FxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Formas 3D feitas de vertices (sem textura propria):
 *  - brilho aditivo (RenderType.lightning): esfera, pilar, espinho, raio, anel, cupula, funil, disco
 *  - SOLID (RenderType.entityTranslucent + textura branca do jogo): pedras, monolitos e esferas escuras, com sombreamento
 * Cada quadrado e emitido nos dois sentidos, entao aparece de qualquer lado.
 */
public class FxRenderer extends EntityRenderer<FxEntity> {
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");

    public FxRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(FxEntity entity) {
        return new ResourceLocation(ElementalMod.MODID, "textures/entity/circle/outer_fire.png"); // nao usada
    }

    @Override
    public void render(FxEntity e, float entityYaw, float partialTick,
                       PoseStack ps, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTick - e.getDelay(); // tempo desde que a peca "acordou"
        if (age < 0.0F) return;
        int max = Math.max(1, e.getMaxAge() - e.getDelay());
        float t = Mth.clamp(age / max, 0.0F, 1.0F);
        float fadeIn = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((max - age) / Math.max(4.0F, max * 0.25F), 0.0F, 1.0F);
        float alpha = fadeIn * fadeOut;

        int style = e.getStyle();
        float rad = e.getRadius();
        float hei = e.getHeight();

        if ((style & FxEntity.EXPAND) != 0) {
            float k = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            rad *= 0.15F + 0.85F * k;
        }
        if ((style & FxEntity.RISE) != 0) {
            float k = Mth.clamp(age / Math.max(3.0F, max * 0.2F), 0.0F, 1.0F);
            hei *= 1.0F - (1.0F - k) * (1.0F - k);
        }
        if ((style & FxEntity.PULSE) != 0) {
            rad *= 1.0F + 0.1F * Mth.sin(age * 0.8F);
        }
        if ((style & FxEntity.FLICKER) != 0) {
            alpha *= 0.7F + 0.3F * Mth.sin(age * 3.1F + e.getId() * 1.7F);
        }

        // curva de crescimento
        float gs = 1.0F;
        switch (e.getGrow()) {
            case FxEntity.POP -> {
                float u = Mth.clamp(age / Math.max(3.0F, max * 0.25F), 0.0F, 1.0F);
                float v = u - 1.0F;
                gs = 1.0F + 2.70158F * v * v * v + 1.70158F * v * v; // easeOutBack: passa um pouco do tamanho e volta
            }
            case FxEntity.SLOW -> gs = t * t * (3.0F - 2.0F * t);
            case FxEntity.SHRINK -> gs = 1.0F - t * t;
            case FxEntity.BELL -> gs = Mth.sin(t * (float) Math.PI);
            default -> { }
        }
        rad *= gs;
        hei *= gs;
        if (alpha <= 0.01F || rad <= 0.001F) return;

        int color = e.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        float dx = e.getDirX(), dy = e.getDirY(), dz = e.getDirZ();
        if (dx * dx + dy * dy + dz * dz < 1.0E-6F) {
            dx = 0.0F;
            dy = 1.0F;
            dz = 0.0F;
        }

        ps.pushPose();
        // deslocamento: velocidade, aceleracao vertical e orbita
        double ox = e.getVx() * age;
        double oy = e.getVy() * age + 0.5 * e.getAy() * age * age;
        double oz = e.getVz() * age;
        if (e.getOrbitR() != 0.0F) {
            double th = Math.toRadians(e.getOrbitPh() + e.getOrbitW() * age);
            ox += e.getOrbitR() * Math.cos(th);
            oz += e.getOrbitR() * Math.sin(th);
        }
        ps.translate(ox, oy, oz);

        ps.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, dx, dy, dz));
        if ((style & FxEntity.SPIN) != 0) {
            float rate = e.getSpinRate() == 0.0F ? 9.0F : e.getSpinRate();
            ps.mulPose(Axis.YP.rotationDegrees(age * rate));
        }
        Matrix4f m = ps.last().pose();
        Matrix3f nm = ps.last().normal();

        if ((style & FxEntity.SOLID) != 0) {
            Emitter em = new Emitter(buffer.getBuffer(RenderType.entityTranslucent(WHITE)), m, nm, true);
            draw(em, e.getShape(), rad, hei, age, r, g, b, Math.min(1.0F, alpha), 1.0F);
        } else {
            float cr = r + (1.0F - r) * 0.55F; // nucleo mais claro
            float cg = g + (1.0F - g) * 0.55F;
            float cb = b + (1.0F - b) * 0.55F;
            Emitter em = new Emitter(buffer.getBuffer(RenderType.lightning()), m, nm, false);
            draw(em, e.getShape(), rad, hei, age, r, g, b, alpha * 0.32F, 1.0F);
            draw(em, e.getShape(), rad, hei, age, cr, cg, cb, alpha * 0.8F, 0.5F);
        }

        ps.popPose();
    }

    private static void draw(Emitter em, int shape, float rad, float hei, float age,
                             float cr, float cg, float cb, float a, float k) {
        switch (shape) {
            case FxEntity.SPHERE -> sphere(em, cr, cg, cb, rad * k, 8, 12, a);
            case FxEntity.PILLAR -> cylinder(em, cr, cg, cb, rad * k, rad * k * 0.75F, hei, a, a * 0.15F, 12);
            case FxEntity.SPIKE -> cylinder(em, cr, cg, cb, rad * k, 0.0F, hei, a, a * 0.6F, 10);
            case FxEntity.BEAM -> cylinder(em, cr, cg, cb, rad * k, rad * k, hei, a, a, 8);
            case FxEntity.RING -> torus(em, cr, cg, cb, rad, Math.max(0.03F, hei) * k, 24, 6, a);
            case FxEntity.DOME -> dome(em, cr, cg, cb, rad * (k < 1.0F ? 0.92F : 1.0F), 6, 14, a);
            case FxEntity.FUNNEL -> funnel(em, cr, cg, cb, rad * k, hei, age, a);
            case FxEntity.DISC -> annulus(em, cr, cg, cb, hei, rad, 24, a * (k < 1.0F ? 0.6F : 1.0F));
            case FxEntity.BOX -> box(em, cr, cg, cb, rad * k, rad * k * 0.9F, hei, a);
            case FxEntity.ROCK -> rock(em, cr, cg, cb, rad * k, a);
            default -> { }
        }
    }

    // ------------------------------------------------------------ emissor de vertices
    /** Emite quadrados em dois modos: brilho (POSITION_COLOR) ou solido com normal (formato de entidade). */
    private static final class Emitter {
        final VertexConsumer vc;
        final Matrix4f m;
        final Matrix3f nm;
        final boolean solid;

        Emitter(VertexConsumer vc, Matrix4f m, Matrix3f nm, boolean solid) {
            this.vc = vc;
            this.m = m;
            this.nm = nm;
            this.solid = solid;
        }

        private void glow(float r, float g, float b, float x, float y, float z, float a) {
            vc.vertex(m, x, y, z).color(r, g, b, a).endVertex();
        }

        private void sv(float r, float g, float b, float x, float y, float z, float a,
                        float nx, float ny, float nz) {
            vc.vertex(m, x, y, z).color(r, g, b, a).uv(0.0F, 0.0F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(nm, nx, ny, nz).endVertex();
        }

        void quad(float r, float g, float b,
                  float x1, float y1, float z1, float a1,
                  float x2, float y2, float z2, float a2,
                  float x3, float y3, float z3, float a3,
                  float x4, float y4, float z4, float a4) {
            if (!solid) {
                glow(r, g, b, x1, y1, z1, a1);
                glow(r, g, b, x2, y2, z2, a2);
                glow(r, g, b, x3, y3, z3, a3);
                glow(r, g, b, x4, y4, z4, a4);
                glow(r, g, b, x4, y4, z4, a4);
                glow(r, g, b, x3, y3, z3, a3);
                glow(r, g, b, x2, y2, z2, a2);
                glow(r, g, b, x1, y1, z1, a1);
                return;
            }
            float ux = x2 - x1, uy = y2 - y1, uz = z2 - z1;
            float vx = x4 - x1, vy = y4 - y1, vz = z4 - z1;
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-6F) {
                nx = 0.0F;
                ny = 1.0F;
                nz = 0.0F;
            } else {
                nx /= len;
                ny /= len;
                nz /= len;
            }
            sv(r, g, b, x1, y1, z1, a1, nx, ny, nz);
            sv(r, g, b, x2, y2, z2, a2, nx, ny, nz);
            sv(r, g, b, x3, y3, z3, a3, nx, ny, nz);
            sv(r, g, b, x4, y4, z4, a4, nx, ny, nz);
            sv(r, g, b, x4, y4, z4, a4, -nx, -ny, -nz);
            sv(r, g, b, x3, y3, z3, a3, -nx, -ny, -nz);
            sv(r, g, b, x2, y2, z2, a2, -nx, -ny, -nz);
            sv(r, g, b, x1, y1, z1, a1, -nx, -ny, -nz);
        }
    }

    // ------------------------------------------------------------ malhas
    private static void cylinder(Emitter e, float r, float g, float b,
                                 float rb, float rt, float h, float ab, float at, int seg) {
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (2 * Math.PI * i / seg);
            float a1 = (float) (2 * Math.PI * (i + 1) / seg);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            e.quad(r, g, b,
                    rb * c0, 0, rb * s0, ab,
                    rb * c1, 0, rb * s1, ab,
                    rt * c1, h, rt * s1, at,
                    rt * c0, h, rt * s0, at);
        }
    }

    private static void sphere(Emitter e, float r, float g, float b,
                               float rad, int stacks, int slices, float a) {
        for (int i = 0; i < stacks; i++) {
            float p0 = (float) (Math.PI * i / stacks - Math.PI / 2);
            float p1 = (float) (Math.PI * (i + 1) / stacks - Math.PI / 2);
            float y0 = rad * Mth.sin(p0), y1 = rad * Mth.sin(p1);
            float r0 = rad * Mth.cos(p0), r1 = rad * Mth.cos(p1);
            for (int j = 0; j < slices; j++) {
                float t0 = (float) (2 * Math.PI * j / slices);
                float t1 = (float) (2 * Math.PI * (j + 1) / slices);
                float c0 = Mth.cos(t0), s0 = Mth.sin(t0), c1 = Mth.cos(t1), s1 = Mth.sin(t1);
                e.quad(r, g, b,
                        r0 * c0, y0, r0 * s0, a,
                        r0 * c1, y0, r0 * s1, a,
                        r1 * c1, y1, r1 * s1, a,
                        r1 * c0, y1, r1 * s0, a);
            }
        }
    }

    private static void dome(Emitter e, float r, float g, float b,
                             float rad, int stacks, int slices, float a) {
        for (int i = 0; i < stacks; i++) {
            float p0 = (float) (Math.PI / 2 * i / stacks);
            float p1 = (float) (Math.PI / 2 * (i + 1) / stacks);
            float y0 = rad * Mth.sin(p0), y1 = rad * Mth.sin(p1);
            float r0 = rad * Mth.cos(p0), r1 = rad * Mth.cos(p1);
            float al0 = a * (1.0F - 0.5F * i / stacks), al1 = a * (1.0F - 0.5F * (i + 1) / stacks);
            for (int j = 0; j < slices; j++) {
                float t0 = (float) (2 * Math.PI * j / slices);
                float t1 = (float) (2 * Math.PI * (j + 1) / slices);
                float c0 = Mth.cos(t0), s0 = Mth.sin(t0), c1 = Mth.cos(t1), s1 = Mth.sin(t1);
                e.quad(r, g, b,
                        r0 * c0, y0, r0 * s0, al0,
                        r0 * c1, y0, r0 * s1, al0,
                        r1 * c1, y1, r1 * s1, al1,
                        r1 * c0, y1, r1 * s0, al1);
            }
        }
    }

    private static void torus(Emitter e, float r, float g, float b,
                              float big, float tube, int seg, int sides, float a) {
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (2 * Math.PI * i / seg);
            float a1 = (float) (2 * Math.PI * (i + 1) / seg);
            float ca0 = Mth.cos(a0), sa0 = Mth.sin(a0), ca1 = Mth.cos(a1), sa1 = Mth.sin(a1);
            for (int j = 0; j < sides; j++) {
                float b0 = (float) (2 * Math.PI * j / sides);
                float b1 = (float) (2 * Math.PI * (j + 1) / sides);
                float rr0 = big + tube * Mth.cos(b0), y0 = tube * Mth.sin(b0);
                float rr1 = big + tube * Mth.cos(b1), y1 = tube * Mth.sin(b1);
                e.quad(r, g, b,
                        rr0 * ca0, y0, rr0 * sa0, a,
                        rr1 * ca0, y1, rr1 * sa0, a,
                        rr1 * ca1, y1, rr1 * sa1, a,
                        rr0 * ca1, y0, rr0 * sa1, a);
            }
        }
    }

    private static void annulus(Emitter e, float r, float g, float b,
                                float rin, float rout, int seg, float a) {
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (2 * Math.PI * i / seg);
            float a1 = (float) (2 * Math.PI * (i + 1) / seg);
            float ca0 = Mth.cos(a0), sa0 = Mth.sin(a0), ca1 = Mth.cos(a1), sa1 = Mth.sin(a1);
            e.quad(r, g, b,
                    rin * ca0, 0, rin * sa0, a,
                    rout * ca0, 0, rout * sa0, a * 0.3F,
                    rout * ca1, 0, rout * sa1, a * 0.3F,
                    rin * ca1, 0, rin * sa1, a);
        }
    }

    private static void funnel(Emitter e, float r, float g, float b,
                               float top, float h, float age, float a) {
        int stacks = 6, slices = 12;
        float bottom = 0.15F;
        for (int s = 0; s < stacks; s++) {
            float f0 = s / (float) stacks, f1 = (s + 1) / (float) stacks;
            float r0 = bottom + (top - bottom) * f0, r1 = bottom + (top - bottom) * f1;
            float y0 = h * f0, y1 = h * f1;
            float tw0 = f0 * 1.2F + age * 0.25F, tw1 = f1 * 1.2F + age * 0.25F;
            float al0 = a * (1.0F - 0.5F * f0), al1 = a * (1.0F - 0.5F * f1);
            for (int j = 0; j < slices; j++) {
                float t0 = (float) (2 * Math.PI * j / slices);
                float t1 = (float) (2 * Math.PI * (j + 1) / slices);
                e.quad(r, g, b,
                        r0 * Mth.cos(t0 + tw0), y0, r0 * Mth.sin(t0 + tw0), al0,
                        r0 * Mth.cos(t1 + tw0), y0, r0 * Mth.sin(t1 + tw0), al0,
                        r1 * Mth.cos(t1 + tw1), y1, r1 * Mth.sin(t1 + tw1), al1,
                        r1 * Mth.cos(t0 + tw1), y1, r1 * Mth.sin(t0 + tw1), al1);
            }
        }
    }

    /** Coluna retangular (monolito de pedra / cubo). half = meia largura da base, halfTop = do topo. */
    private static void box(Emitter e, float r, float g, float b, float half, float halfTop, float h, float a) {
        float[][] base = {{-half, -half}, {half, -half}, {half, half}, {-half, half}};
        float[][] top = {{-halfTop, -halfTop}, {halfTop, -halfTop}, {halfTop, halfTop}, {-halfTop, halfTop}};
        for (int i = 0; i < 4; i++) {
            int n = (i + 1) % 4;
            e.quad(r, g, b,
                    base[i][0], 0, base[i][1], a,
                    base[n][0], 0, base[n][1], a,
                    top[n][0], h, top[n][1], a,
                    top[i][0], h, top[i][1], a);
        }
        e.quad(r, g, b,
                top[0][0], h, top[0][1], a, top[1][0], h, top[1][1], a,
                top[2][0], h, top[2][1], a, top[3][0], h, top[3][1], a);
        e.quad(r, g, b,
                base[0][0], 0, base[0][1], a, base[1][0], 0, base[1][1], a,
                base[2][0], 0, base[2][1], a, base[3][0], 0, base[3][1], a);
    }

    /** Pedra irregular (esfera de poucas faces com os vertices deformados). */
    private static void rock(Emitter e, float r, float g, float b, float rad, float a) {
        int stacks = 3, slices = 6;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                float[] p00 = rockPoint(i, j, stacks, slices, rad);
                float[] p01 = rockPoint(i, j + 1, stacks, slices, rad);
                float[] p11 = rockPoint(i + 1, j + 1, stacks, slices, rad);
                float[] p10 = rockPoint(i + 1, j, stacks, slices, rad);
                e.quad(r, g, b,
                        p00[0], p00[1], p00[2], a, p01[0], p01[1], p01[2], a,
                        p11[0], p11[1], p11[2], a, p10[0], p10[1], p10[2], a);
            }
        }
    }

    private static float[] rockPoint(int i, int j, int stacks, int slices, float rad) {
        int jj = (i == 0 || i == stacks) ? 0 : (j % slices);
        float jitter = 0.72F + 0.5F * (float) Mth.frac(Mth.sin(i * 12.9898F + jj * 78.233F) * 43758.547F);
        float phi = (float) (Math.PI * i / stacks - Math.PI / 2);
        float theta = (float) (2 * Math.PI * (j % slices) / slices);
        float rr = rad * jitter;
        return new float[]{rr * Mth.cos(phi) * Mth.cos(theta), rr * Mth.sin(phi), rr * Mth.cos(phi) * Mth.sin(theta)};
    }
}
