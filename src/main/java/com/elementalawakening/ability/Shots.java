package com.elementalawakening.ability;

import com.elementalawakening.element.Element;
import com.elementalawakening.element.ElementStorage;
import com.elementalawakening.entity.FxEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Projeteis (simulados no servidor, com o modelo 3D do elemento acompanhando) e raios instantaneos. */
public final class Shots {
    private Shots() {}

    private static int colorOf(ServerPlayer p) {
        Element el = ElementStorage.get(p);
        return el == null ? 0xFFFFFF : el.color;
    }

    /** Projetil que sai do olhar do jogador. onImpact(posicao, alvo) — alvo e null se bateu em bloco/acabou o alcance. */
    public static void shoot(ServerPlayer p, double speed, int maxTicks, ParticleOptions trail,
                             BiConsumer<Vec3, LivingEntity> onImpact) {
        Vec3 dir = p.getLookAngle();
        shootFrom(p, p.getEyePosition().add(dir.scale(0.8)), dir, speed, maxTicks, trail, onImpact);
    }

    public static void shootFrom(ServerPlayer p, Vec3 start, Vec3 dir, double speed, int maxTicks,
                                 ParticleOptions trail, BiConsumer<Vec3, LivingEntity> onImpact) {
        ServerLevel level = p.serverLevel();
        Vec3 d = dir.normalize();
        Element el = ElementStorage.get(p);
        ElementFx.ProjModel m = ElementFx.projModel(el);
        int c1 = m.color() == -1 ? colorOf(p) : m.color();

        FxEntity main = Fx.make(level, m.shape(), start, d, c1, m.radius(), m.height(), maxTicks + 20, m.style());
        FxEntity tail = null;
        if (m.shape2() >= 0) {
            int c2 = m.color2() == -1 ? colorOf(p) : m.color2();
            tail = Fx.make(level, m.shape2(), start, m.back() ? d.scale(-1) : d, c2,
                    m.radius2(), m.height2(), maxTicks + 20, m.style2());
        }
        step(p, level, start, d, speed, maxTicks, trail, onImpact, main, tail, m.back());
    }

    private static void step(ServerPlayer p, ServerLevel level, Vec3 pos, Vec3 dir, double speed, int left,
                             ParticleOptions trail, BiConsumer<Vec3, LivingEntity> onImpact,
                             FxEntity main, FxEntity tail, boolean tailBack) {
        Vec3 next = pos.add(dir.scale(speed));
        BlockHitResult block = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, p));
        boolean blocked = block.getType() != HitResult.Type.MISS;
        Vec3 end = blocked ? block.getLocation() : next;

        LivingEntity hitEntity = null;
        double best = Double.MAX_VALUE;
        AABB box = new AABB(pos, end).inflate(0.7);
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> x != p && x.isAlive() && !x.isSpectator())) {
            double d = t.position().add(0, t.getBbHeight() / 2, 0).distanceToSqr(pos);
            if (d < best) {
                best = d;
                hitEntity = t;
            }
        }

        level.sendParticles(trail, end.x, end.y, end.z, 3, 0.1, 0.1, 0.1, 0.01);
        Element shooterEl = ElementStorage.get(p);
        if (shooterEl != null) {
            level.sendParticles(ElementFx.p2(shooterEl), end.x, end.y, end.z, 2, 0.18, 0.18, 0.18, 0.03);
        }
        main.setPos(end.x, end.y, end.z);
        main.setDir(dir);
        if (tail != null) {
            tail.setPos(end.x, end.y, end.z);
            tail.setDir(tailBack ? dir.scale(-1) : dir);
        }

        if (hitEntity != null) {
            main.discard();
            if (tail != null) tail.discard();
            onImpact.accept(hitEntity.position().add(0, hitEntity.getBbHeight() / 2, 0), hitEntity);
            return;
        }
        if (blocked || left <= 1) {
            main.discard();
            if (tail != null) tail.discard();
            onImpact.accept(end, null);
            return;
        }
        Scheduler.run(1, () -> step(p, level, next, dir, speed, left - 1, trail, onImpact, main, tail, tailBack));
    }

    /** Raio instantaneo na direcao do olhar. Devolve o ponto final. */
    public static Vec3 beam(ServerPlayer p, double range, double width, ParticleOptions particle,
                            Consumer<LivingEntity> onHit) {
        return beamFrom(p, p.getEyePosition(), p.getLookAngle(), range, width, particle, onHit);
    }

    /** Raio instantaneo a partir de um ponto qualquer, na direcao dada. */
    public static Vec3 beamFrom(ServerPlayer p, Vec3 start, Vec3 dirIn, double range, double width,
                                ParticleOptions particle, Consumer<LivingEntity> onHit) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = dirIn.normalize();
        Vec3 far = start.add(dir.scale(range));
        BlockHitResult block = level.clip(new ClipContext(start, far, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, p));
        Vec3 end = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        double len = start.distanceTo(end);

        // cilindro 3D brilhante ao longo do raio
        Fx.make(level, FxEntity.BEAM, start.add(0, -0.2, 0).add(dir.scale(0.6)), dir, colorOf(p),
                (float) (width * 0.35 + 0.08), (float) Math.max(0.5, len - 0.6), 8, 0);

        for (double t = 1.0; t < len; t += 0.5) {
            Vec3 q = start.add(dir.scale(t));
            level.sendParticles(particle, q.x, q.y - 0.15, q.z, 1, 0, 0, 0, 0);
        }

        List<LivingEntity> hits = new ArrayList<>();
        AABB box = new AABB(start, end).inflate(width + 1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                x -> x != p && x.isAlive() && !x.isSpectator())) {
            Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0).subtract(start);
            double proj = Mth.clamp(c.dot(dir), 0.0, len);
            if (c.distanceTo(dir.scale(proj)) <= width + e.getBbWidth() / 2) {
                hits.add(e);
            }
        }
        for (LivingEntity e : hits) onHit.accept(e);
        return end;
    }
}
