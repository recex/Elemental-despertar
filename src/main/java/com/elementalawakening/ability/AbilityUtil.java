package com.elementalawakening.ability;

import com.elementalawakening.element.Element;
import com.elementalawakening.entity.MagicCircleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public final class AbilityUtil {
    public static final DustParticleOptions BLOOD_DUST = new DustParticleOptions(new Vector3f(0.75F, 0.05F, 0.1F), 1.4F);

    private AbilityUtil() {}

    // ------------------------------------------------------------ mira / alvos
    /** Ponto para onde o jogador esta olhando (o "cursor"), ate 'range' blocos. */
    public static Vec3 cursor(ServerPlayer player, double range) {
        HitResult hit = player.pick(range, 1.0F, false);
        return hit.getLocation();
    }

    /** Ponto final do olhar, parando antes de bloco (para dash/teleporte). Devolve a posicao dos OLHOS. */
    public static Vec3 rayEnd(ServerPlayer p, double dist) {
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 far = start.add(look.scale(dist));
        BlockHitResult h = p.serverLevel().clip(new ClipContext(start, far, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, p));
        return h.getType() == HitResult.Type.MISS ? far : h.getLocation().subtract(look.scale(0.6));
    }

    /** Seres vivos num cilindro ao redor de c (exclui o proprio jogador). */
    public static List<LivingEntity> around(ServerPlayer player, Vec3 c, double radius, double height) {
        double rad = radius * Charge.areaMult(player);
        AABB box = new AABB(c.x - rad, c.y - 1.0, c.z - rad, c.x + rad, c.y + height, c.z + rad);
        double r2 = rad * rad;
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == player || !e.isAlive() || e.isSpectator()) return false;
            double dx = e.getX() - c.x;
            double dz = e.getZ() - c.z;
            return dx * dx + dz * dz <= r2;
        });
    }

    /** Seres vivos num cone a frente do jogador (cosHalf = cosseno do meio-angulo). */
    public static List<LivingEntity> cone(ServerPlayer p, double range, double cosHalf) {
        Vec3 eye = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range), t -> {
            if (t == p || !t.isAlive() || t.isSpectator()) return false;
            Vec3 v = t.position().add(0, t.getBbHeight() / 2, 0).subtract(eye);
            double len = v.length();
            if (len > range || len < 1.0E-3) return false;
            return v.dot(dir) / len >= cosHalf;
        });
    }

    /** Alvo mais proximo da linha do olhar (ate 'range' blocos), ou null. */
    public static LivingEntity targetEntity(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        LivingEntity best = null;
        double bestD = range;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(range), t -> t != p && t.isAlive() && !t.isSpectator())) {
            Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0).subtract(start);
            double proj = c.dot(dir);
            if (proj < 0 || proj > bestD) continue;
            if (c.distanceTo(dir.scale(proj)) <= 1.2 + e.getBbWidth() / 2) {
                best = e;
                bestD = proj;
            }
        }
        return best;
    }

    public static Vec3 flat(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : h.normalize();
    }

    /** Desce ate o chao a partir de pos (para posicionar circulos). */
    public static Vec3 ground(ServerLevel level, Vec3 pos) {
        BlockPos bp = BlockPos.containing(pos.x, pos.y + 0.3, pos.z);
        for (int i = 0; i < 8; i++) {
            BlockPos b = bp.below(i);
            if (!level.getBlockState(b).getCollisionShape(level, b).isEmpty()) {
                return new Vec3(pos.x, b.getY() + 1.0, pos.z);
            }
        }
        return pos;
    }

    public static Vec3 randomInDisc(RandomSource r, Vec3 c, double radius) {
        double a = r.nextDouble() * Math.PI * 2;
        double d = Math.sqrt(r.nextDouble()) * radius;
        return new Vec3(c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d);
    }

    // ------------------------------------------------------------ dano e efeitos
    public static void hit(ServerPlayer player, LivingEntity target, float damage) {
        target.invulnerableTime = 0; // permite golpes seguidos
        target.hurt(player.damageSources().playerAttack(player), damage * Charge.dmgMult(player));
    }

    /** "Atordoar": impede de andar por alguns ticks. */
    public static void stun(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false));
    }

    public static void launch(LivingEntity target, double x, double y, double z) {
        target.setDeltaMovement(x, y, z);
        target.hurtMarked = true;
    }

    /** Explosao generica: dano + atordoamento + arremesso (up = para cima, out = para fora). */
    public static int blast(ServerPlayer p, Vec3 c, double r, float dmg, int stun, double up, double out) {
        Element el = com.elementalawakening.element.ElementStorage.get(p);
        if (el != null && r >= 1.2) {
            Fx.impact(p.serverLevel(), ground(p.serverLevel(), c), el, (float) (r * Charge.areaMult(p)));
            ElementFx.burst(p.serverLevel(), el, c, (float) (r * Charge.areaMult(p)));
        }
        List<LivingEntity> list = around(p, c, r, 4);
        for (LivingEntity e : list) {
            if (dmg > 0) hit(p, e, dmg);
            if (stun > 0) stun(e, stun);
            if (up != 0 || out != 0) {
                Vec3 d = e.position().subtract(c).multiply(1, 0, 1);
                d = d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize();
                launch(e, d.x * out, up, d.z * out);
            }
        }
        return list.size();
    }

    public static void pull(LivingEntity e, Vec3 center, double strength) {
        Vec3 d = center.subtract(e.position());
        if (d.lengthSqr() < 0.25) return;
        d = d.normalize().scale(strength);
        launch(e, d.x, d.y * 0.5 + 0.05, d.z);
    }

    /** Custa uma fracao da vida maxima; falha se nao tiver vida suficiente. */
    public static boolean payHealth(ServerPlayer p, float fraction) {
        float cost = p.getMaxHealth() * fraction;
        if (!p.isCreative() && p.getHealth() <= cost + 1.0F) {
            actionbar(p, "Vida insuficiente.");
            return false;
        }
        p.hurt(p.damageSources().magic(), cost);
        return true;
    }

    public static void teleport(ServerPlayer p, Vec3 feetPos) {
        ServerLevel level = p.serverLevel();
        sound(level, p.position(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(),
                30, 0.4, 0.8, 0.4, 0.3);
        p.teleportTo(feetPos.x, feetPos.y, feetPos.z);
        p.resetFallDistance();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, feetPos.x, feetPos.y + 1, feetPos.z,
                30, 0.4, 0.8, 0.4, 0.3);
    }

    public static void lightning(ServerLevel level, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(pos.x, pos.y, pos.z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    // ------------------------------------------------------------ blocos temporarios
    public static void tempBlock(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (!level.getBlockState(pos).isAir()) return;
        level.setBlockAndUpdate(pos, state);
        Scheduler.run(ticks, () -> {
            if (level.getBlockState(pos).is(state.getBlock())) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        });
    }

    // ------------------------------------------------------------ visual / som
    public static void circle(ServerLevel level, Vec3 pos, Element element, float sizeIn, int lifetime) {
        float size = sizeIn * Charge.syncAreaMult(); // golpe carregado = circulo maior
        Vec3 g = ground(level, pos);
        MagicCircleEntity.spawn(level, g, element, size, lifetime);
        if (size >= 5.0F && lifetime >= 30) {
            com.elementalawakening.fx.Cine.play(level, g, element, size, lifetime); // sequencia cinematografica
        } else {
            Fx.signature(level, g, element, size, lifetime); // estrutura 3D curta do elemento
        }
    }

    public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void fx(ServerLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    public static void column(ServerLevel level, ParticleOptions particle, Vec3 c, double height, int perStep, double spread) {
        for (double h = 0; h < height; h += 0.5) {
            level.sendParticles(particle, c.x, c.y + h, c.z, perStep, spread, 0.1, spread, 0.02);
        }
    }

    public static void line(ServerLevel level, ParticleOptions particle, Vec3 a, Vec3 b, int points) {
        for (int i = 0; i <= points; i++) {
            Vec3 q = a.lerp(b, i / (double) points);
            level.sendParticles(particle, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    public static void ring(ServerLevel level, Vec3 c, double radius, ParticleOptions particle) {
        for (int deg = 0; deg < 360; deg += 12) {
            double rad = Math.toRadians(deg);
            level.sendParticles(particle, c.x + Math.cos(rad) * radius, c.y + 0.2, c.z + Math.sin(rad) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    public static void actionbar(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }
}
