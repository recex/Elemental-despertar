package com.elementalawakening.client;

import com.elementalawakening.ElementalMod;
import com.elementalawakening.element.Element;
import com.elementalawakening.entity.MagicCircleEntity;
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

import java.util.Locale;

/**
 * Circulo magico em varias camadas com profundidade:
 *  - no chao: aneis/runas, trelica ornamentada e figura do elemento girando em sentidos opostos
 *  - um segundo "andar" menor flutuando acima (aneis + trelica) com leve balanco
 *  - o emblema do elemento em pe, girando como um holograma
 *  - 6 runas (o emblema em miniatura) orbitando na borda
 *  - abertura com giro rapido que desacelera e "pop" de escala; fechamento suave
 * Circulos em pe (relogio do Tempo, circulo de conjuracao) usam so as camadas principais.
 */
public class MagicCircleRenderer extends EntityRenderer<MagicCircleEntity> {
    private static final ResourceLocation[] OUTER = build("outer");
    private static final ResourceLocation[] FRAME = build("frame");
    private static final ResourceLocation[] EMBLEM = build("emblem");
    private static final ResourceLocation[] LATTICE = build("lattice");

    private static ResourceLocation[] build(String kind) {
        Element[] all = Element.values();
        ResourceLocation[] out = new ResourceLocation[all.length];
        for (int i = 0; i < all.length; i++) {
            out[i] = new ResourceLocation(ElementalMod.MODID,
                    "textures/entity/circle/" + kind + "_" + all[i].name().toLowerCase(Locale.ROOT) + ".png");
        }
        return out;
    }

    public MagicCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicCircleEntity entity) {
        return OUTER[Mth.clamp(entity.getElementId(), 0, OUTER.length - 1)];
    }

    @Override
    public void render(MagicCircleEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        int maxAge = Math.max(1, entity.getMaxAge());

        // abertura: cresce com "pop" (passa um pouco do tamanho e volta); fechamento: encolhe suave
        float openT = Mth.clamp(age / 10.0F, 0.0F, 1.0F);
        float v = openT - 1.0F;
        float open = 1.0F + 2.70158F * v * v * v + 1.70158F * v * v;
        float closeT = Mth.clamp((maxAge - age) / 8.0F, 0.0F, 1.0F);
        float alpha = Mth.clamp(age / 4.0F, 0.0F, 1.0F) * closeT
                * (0.88F + 0.12F * Mth.sin(age * 0.6F + entity.getId()));
        if (alpha <= 0.01F) return;

        float s = entity.getSize() * Math.max(0.01F, open) * (0.8F + 0.2F * closeT);
        int id = Mth.clamp(entity.getElementId(), 0, OUTER.length - 1);

        int color = entity.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        boolean vertical = Math.abs(entity.getXRot()) > 1.0F;
        float boost = 40.0F * (1.0F - (float) Math.exp(-age / 3.0F)); // giro rapido que desacelera ao abrir
        float hs = 0.3F + s * 0.08F;                                  // escala das alturas das camadas
        float bob = Mth.sin(age * 0.15F) * 0.08F * hs * 4.0F;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        // --- andar principal (chao) ---
        drawH(pose, buffer, OUTER[id], s, age * 1.4F + boost, 0.00F, r, g, b, alpha);
        drawH(pose, buffer, LATTICE[id], s * 0.86F, -(age * 2.2F + boost * 1.5F), 0.07F * hs, r, g, b, alpha * 0.9F);
        drawH(pose, buffer, FRAME[id], s * 0.97F, -(age * 2.5F + boost), 0.14F * hs, r, g, b, alpha);
        drawH(pose, buffer, EMBLEM[id], s * (100.0F / 256.0F), age * 0.8F, 0.22F * hs, r, g, b, alpha);

        if (!vertical) {
            // --- segundo andar flutuando ---
            drawH(pose, buffer, OUTER[id], s * 0.55F, -(age * 1.8F + boost), 0.9F * hs + bob, r, g, b, alpha * 0.7F);
            drawH(pose, buffer, LATTICE[id], s * 0.40F, age * 3.4F + boost * 2.0F, 1.6F * hs + bob, r, g, b, alpha * 0.55F);

            // --- emblema em pe, girando como holograma ---
            float holoY = s * 0.5F + 0.6F * hs + bob;
            drawV(pose, buffer, EMBLEM[id], s * 0.34F, age * 3.0F, 0.0F, holoY, 0.0F, r, g, b, alpha * 0.85F);

            // --- runas orbitando na borda ---
            for (int k = 0; k < 6; k++) {
                double th = Math.toRadians(age * 2.0F + k * 60.0F);
                double wave = Math.sin(age * 0.1F + k) * 0.12F * s;
                double rr = s * (0.9F + 0.04F * Mth.sin(age * 0.2F + k));
                drawV(pose, buffer, EMBLEM[id], s * 0.1F, (float) (90.0 - Math.toDegrees(th)),
                        (float) (Math.cos(th) * rr), (float) (hs * 0.8F + 0.25F * s + wave), (float) (Math.sin(th) * rr),
                        r, g, b, alpha * 0.8F);
            }
        }
        pose.popPose();
    }

    /** Camada horizontal (plano XZ), vista de cima. */
    private static void drawH(PoseStack pose, MultiBufferSource buffer, ResourceLocation texture,
                              float size, float angleDeg, float y, float r, float g, float b, float a) {
        pose.pushPose();
        pose.translate(0.0, y, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(angleDeg));
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        Matrix3f n = last.normal();
        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(texture)); // eyes = brilho aditivo

        vertex(vc, m, n, -size, 0.0F, -size, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, r, g, b, a);
        vertex(vc, m, n, -size, 0.0F, size, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, r, g, b, a);
        vertex(vc, m, n, size, 0.0F, size, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, r, g, b, a);
        vertex(vc, m, n, size, 0.0F, -size, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, r, g, b, a);
        pose.popPose();
    }

    /** Placa vertical (plano XY) girada em torno do eixo Y; desenhada nos dois sentidos. */
    private static void drawV(PoseStack pose, MultiBufferSource buffer, ResourceLocation texture,
                              float size, float yawDeg, float x, float y, float z,
                              float r, float g, float b, float a) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        Matrix3f n = last.normal();
        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(texture));

        vertex(vc, m, n, -size, size, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, r, g, b, a);
        vertex(vc, m, n, -size, -size, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, r, g, b, a);
        vertex(vc, m, n, size, -size, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, r, g, b, a);
        vertex(vc, m, n, size, size, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, r, g, b, a);
        // verso (ordem invertida)
        vertex(vc, m, n, size, size, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, r, g, b, a);
        vertex(vc, m, n, size, -size, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, -1.0F, r, g, b, a);
        vertex(vc, m, n, -size, -size, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, r, g, b, a);
        vertex(vc, m, n, -size, size, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, r, g, b, a);
        pose.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz,
                               float r, float g, float b, float a) {
        vc.vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}
