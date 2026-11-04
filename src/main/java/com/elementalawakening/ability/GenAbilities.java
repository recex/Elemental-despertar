package com.elementalawakening.ability;

import com.elementalawakening.element.Element;
import com.elementalawakening.entity.FxEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Golpes dos 30 elementos novos, montados a partir de "tipos" de golpe (projetil, raio, explosao, zona, pilar,
 * chuva, investida, atracao, buff, teletransporte, sentinela, corrente). Cada elemento tem 4 golpes (Z X C V).
 * Valores (dano, raio, cooldown) sao nossos e podem ser ajustados na tabela do final do arquivo.
 */
public final class GenAbilities {
    private interface Run {
        boolean run(ServerPlayer p, Element el);
    }

    private record Atk(String name, int cd, Run run) {}

    /** Efeito extra de um golpe: efeito de pocao e/ou fogo. */
    private record Eff(MobEffect effect, int ticks, int amp, int fire) {}

    private static final Eff NONE = new Eff(null, 0, 0, 0);
    private static final Map<Element, AbilitySet> SETS = new EnumMap<>(Element.class);

    private GenAbilities() {}

    public static Map<Element, AbilitySet> all() {
        return SETS;
    }

    // ------------------------------------------------------------------ efeitos
    private static Eff fx(MobEffect e, int ticks, int amp) {
        return new Eff(e, ticks, amp, 0);
    }

    private static Eff burn(int seconds) {
        return new Eff(null, 0, 0, seconds);
    }

    private static void apply(LivingEntity e, Eff f) {
        if (f.effect() != null) e.addEffect(new MobEffectInstance(f.effect(), f.ticks(), f.amp()));
        if (f.fire() > 0) e.setSecondsOnFire(f.fire());
    }

    private static void applyAround(ServerPlayer p, Vec3 c, double r, Eff f) {
        if (f.effect() == null && f.fire() <= 0) return;
        for (LivingEntity e : AbilityUtil.around(p, c, r, 4)) apply(e, f);
    }

    // ------------------------------------------------------------------ tipos de golpe
    /** Projetil que explode ao bater. */
    private static Atk proj(String name, int cd, float dmg, float r, double speed, Eff f) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            AbilityUtil.sound(level, p.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.2F);
            Shots.shoot(p, speed, 40, ElementFx.p1(el), (pos, t) -> {
                AbilityUtil.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 0.9F, 1.3F);
                AbilityUtil.blast(p, pos, r, dmg, 0, 0.4, 0.5);
                applyAround(p, pos, r, f);
                AbilityUtil.circle(level, pos, el, r * 1.3F + 1.0F, 36);
            });
            return true;
        });
    }

    /** Raio instantaneo. */
    private static Atk beam(String name, int cd, float dmg, float width, Eff f) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            AbilityUtil.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 0.8F, 1.8F);
            Vec3 end = Shots.beam(p, 40, width, ElementFx.p1(el), e -> {
                AbilityUtil.hit(p, e, dmg);
                apply(e, f);
            });
            ElementFx.burst(level, el, end, 1.5F);
            return true;
        });
    }

    /** Explosao ao redor do jogador. */
    private static Atk burst(String name, int cd, float dmg, float r, int stun, double up, double out, Eff f) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            Vec3 c = p.position();
            AbilityUtil.circle(level, c, el, r * 1.1F + 1.0F, 34);
            AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.8F);
            AbilityUtil.blast(p, c, r, dmg, stun, up, out);
            applyAround(p, c, r, f);
            return true;
        });
    }

    /** Zona no cursor que causa dano varias vezes. */
    private static Atk zone(String name, int cd, float dmg, float r, int seconds, Eff f) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            Vec3 c = AbilityUtil.cursor(p, 30);
            AbilityUtil.circle(level, c, el, r * 1.1F + 1.0F, seconds * 20 + 10);
            AbilityUtil.sound(level, c, SoundEvents.EVOKER_CAST_SPELL, 1.2F, 0.7F);
            Scheduler.repeat(seconds * 2, 10, i -> {
                ElementFx.ambient(level, el, c, r);
                for (LivingEntity e : AbilityUtil.around(p, c, r, 3)) {
                    AbilityUtil.hit(p, e, dmg);
                    apply(e, f);
                }
            });
            return true;
        });
    }

    /** Pilar que sobe no cursor (estilo Eruption). */
    private static Atk pillar(String name, int cd, float dmg, float r, Eff f) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            Vec3 c = AbilityUtil.cursor(p, 30);
            AbilityUtil.circle(level, c, el, r * 1.2F + 1.0F, 50);
            AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.6F);
            Scheduler.repeat(6, 5, i -> {
                AbilityUtil.column(level, ElementFx.p1(el), c, 7, 6, r * 0.4);
                ElementFx.burst(level, el, c.add(0, 1, 0), r * 0.6F);
                for (LivingEntity e : AbilityUtil.around(p, c, r, 7)) {
                    AbilityUtil.hit(p, e, dmg * 0.5F);
                    AbilityUtil.launch(e, e.getDeltaMovement().x, 0.5, e.getDeltaMovement().z);
                    apply(e, f);
                }
            });
            return true;
        });
    }

    /** Varios impactos caindo numa area. */
    private static Atk rain(String name, int cd, float dmg, float r, int count, Eff f) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            Vec3 c = AbilityUtil.cursor(p, 35);
            AbilityUtil.circle(level, c, el, r * 1.1F + 1.0F, count * 4 + 30);
            Scheduler.repeat(count, 4, i -> {
                Vec3 q = AbilityUtil.randomInDisc(p.getRandom(), c, r);
                AbilityUtil.line(level, ElementFx.p1(el), q.add(0, 12, 0), q, 14);
                AbilityUtil.sound(level, q, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.5F);
                AbilityUtil.blast(p, q, 2.0, dmg, 0, 0.3, 0.4);
                applyAround(p, q, 2.0, f);
            });
            return true;
        });
    }

    /** Investida para frente acertando quem estiver no caminho. */
    private static Atk dash(String name, int cd, float dmg, double dist) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            Vec3 start = p.position();
            Vec3 end = AbilityUtil.rayEnd(p, dist).subtract(0, p.getEyeHeight(), 0);
            Set<LivingEntity> victims = new HashSet<>();
            for (int i = 0; i <= 12; i++) victims.addAll(AbilityUtil.around(p, start.lerp(end, i / 12.0), 2, 3));
            AbilityUtil.circle(level, start, el, 2.6F, 24);
            AbilityUtil.line(level, ElementFx.p1(el), start.add(0, 1, 0), end.add(0, 1, 0), 26);
            AbilityUtil.line(level, ElementFx.p2(el), start.add(0, 0.5, 0), end.add(0, 0.5, 0), 26);
            AbilityUtil.teleport(p, end);
            ElementFx.burst(level, el, end, 2.0F);
            Vec3 dir = AbilityUtil.flat(end.subtract(start));
            for (LivingEntity e : victims) {
                AbilityUtil.hit(p, e, dmg);
                AbilityUtil.stun(e, 20);
                AbilityUtil.launch(e, dir.x * 1.1, 0.4, dir.z * 1.1);
            }
            return true;
        });
    }

    /** Esfera que puxa os inimigos e explode no fim. */
    private static Atk pull(String name, int cd, float dmg, float r, int seconds) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            Vec3 c = AbilityUtil.cursor(p, 35);
            Vec3 orb = c.add(0, 2.5, 0);
            AbilityUtil.circle(level, c, el, r * 1.1F + 1.0F, seconds * 20 + 10);
            Fx.make(level, FxEntity.SPHERE, orb, null, el.color, 0.9F, 0.0F, seconds * 20, FxEntity.PULSE);
            AbilityUtil.sound(level, c, SoundEvents.ENDERMAN_TELEPORT, 1.5F, 0.5F);
            Scheduler.repeat(seconds * 2, 10, i -> {
                ElementFx.ambient(level, el, orb, 2.0F);
                for (LivingEntity e : AbilityUtil.around(p, c, r, 7)) {
                    AbilityUtil.pull(e, orb, 0.55);
                    AbilityUtil.hit(p, e, dmg);
                }
            });
            Scheduler.run(seconds * 20, () -> {
                AbilityUtil.sound(level, c, SoundEvents.GENERIC_EXPLODE, 1.5F, 0.7F);
                AbilityUtil.blast(p, c, Math.min(r, 6.0F), dmg * 3.0F, 0, 0.8, 0.9);
            });
            return true;
        });
    }

    /** Fortalece o jogador (-1 = sem aquele efeito). boost = dano 1.5x. */
    private static Atk buff(String name, int cd, int seconds, int speed, int regen, int resist,
                            float heal, boolean boost) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            int ticks = seconds * 20;
            if (speed >= 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, speed));
            if (regen >= 0) p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, regen));
            if (resist >= 0) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, resist));
            if (boost) Buffs.set(p, "traveler", ticks);
            if (heal > 0) p.heal(p.getMaxHealth() * heal);
            AbilityUtil.circle(level, p.position(), el, 2.8F, ticks);
            AbilityUtil.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.2F);
            Scheduler.repeat(seconds * 2, 10, i -> {
                if (p.isAlive()) ElementFx.ambient(level, el, p.position(), 1.0F);
            });
            AbilityUtil.actionbar(p, "Poder ativo por " + seconds + "s");
            return true;
        });
    }

    /** Teletransporte ate o cursor com explosao na chegada. */
    private static Atk teleport(String name, int cd, float dmg, double range) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            AbilityUtil.circle(level, p.position(), el, 2.4F, 24);
            Vec3 end = AbilityUtil.rayEnd(p, range).subtract(0, p.getEyeHeight(), 0);
            AbilityUtil.teleport(p, end);
            AbilityUtil.circle(level, end, el, 3.6F, 30);
            AbilityUtil.blast(p, end, 3.0, dmg, 0, 0.5, 0.8);
            return true;
        });
    }

    /** Sentinela no cursor que ataca o inimigo mais proximo. */
    private static Atk orb(String name, int cd, float dmg, float r, int seconds) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            Vec3 base = AbilityUtil.cursor(p, 30);
            Vec3 orbPos = base.add(0, 1.5, 0);
            AbilityUtil.circle(level, base, el, 3.4F, seconds * 20);
            Fx.make(level, FxEntity.SPHERE, orbPos, null, el.color, 0.5F, 0.0F, seconds * 20, FxEntity.PULSE);
            Scheduler.repeat(seconds * 20 / 15, 15, i -> {
                ElementFx.ambient(level, el, orbPos, 0.8F);
                LivingEntity best = null;
                double bestD = Double.MAX_VALUE;
                for (LivingEntity e : AbilityUtil.around(p, base, r, 6)) {
                    double d = e.position().distanceToSqr(base);
                    if (d < bestD) {
                        bestD = d;
                        best = e;
                    }
                }
                if (best != null) {
                    AbilityUtil.line(level, ElementFx.p1(el), orbPos, best.position().add(0, 1, 0), 14);
                    ElementFx.burst(level, el, best.position().add(0, 1, 0), 0.8F);
                    AbilityUtil.hit(p, best, dmg);
                }
            });
            return true;
        });
    }

    /** Ataque que pula de um inimigo para outro proximo. */
    private static Atk chain(String name, int cd, float dmg, int jumps, Eff f) {
        return new Atk(name, cd, (p, el) -> {
            ServerLevel level = p.serverLevel();
            LivingEntity cur = AbilityUtil.targetEntity(p, 25);
            if (cur == null) {
                double bestD = Double.MAX_VALUE;
                for (LivingEntity e : AbilityUtil.around(p, p.position(), 12, 4)) {
                    double d = e.position().distanceToSqr(p.position());
                    if (d < bestD) {
                        bestD = d;
                        cur = e;
                    }
                }
            }
            if (cur == null) {
                AbilityUtil.actionbar(p, "Sem alvo por perto.");
                return false;
            }
            Set<LivingEntity> seen = new HashSet<>();
            Vec3 from = p.getEyePosition().add(0, -0.2, 0);
            AbilityUtil.sound(level, p.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.5F);
            for (int j = 0; j < jumps && cur != null; j++) {
                seen.add(cur);
                Vec3 center = cur.position().add(0, cur.getBbHeight() / 2, 0);
                AbilityUtil.line(level, ElementFx.p1(el), from, center, 16);
                AbilityUtil.line(level, ElementFx.p2(el), from, center, 8);
                AbilityUtil.hit(p, cur, dmg);
                apply(cur, f);
                ElementFx.burst(level, el, center, 1.0F);
                from = center;

                LivingEntity next = null;
                double bestD = Double.MAX_VALUE;
                List<LivingEntity> near = AbilityUtil.around(p, cur.position(), 8, 4);
                for (LivingEntity e : near) {
                    if (seen.contains(e)) continue;
                    double d = e.position().distanceToSqr(cur.position());
                    if (d < bestD) {
                        bestD = d;
                        next = e;
                    }
                }
                cur = next;
            }
            return true;
        });
    }

    // ------------------------------------------------------------------ montagem
    private static void reg(Element el, Atk... atks) {
        AbilitySet set = new AbilitySet();
        for (Atk a : atks) set.add(a.name(), a.cd(), p -> a.run().run(p, el));
        SETS.put(el, set);
    }

    /** Acrescenta golpes a um elemento que ja tem os 4 primeiros (vao para as teclas B, N). */
    private static void ext(Element el, Atk... atks) {
        AbilitySet set = SETS.get(el);
        for (Atk a : atks) set.add(a.name(), a.cd(), p -> a.run().run(p, el));
    }

    // ------------------------------------------------------------------ TABELA DOS GOLPES
    // proj(nome, cd, dano, raio, velocidade, efeito) | beam(nome, cd, dano, largura, efeito)
    // burst(nome, cd, dano, raio, atordoar, subir, empurrar, efeito) | zone(nome, cd, dano, raio, segundos, efeito)
    // pillar(nome, cd, dano, raio, efeito) | rain(nome, cd, dano, raio, quantidade, efeito)
    // dash(nome, cd, dano, distancia) | pull(nome, cd, dano, raio, segundos)
    // buff(nome, cd, segundos, velocidade, regen, resistencia, cura, danoExtra) | teleport(nome, cd, dano, alcance)
    // orb(nome, cd, dano, raio, segundos) | chain(nome, cd, dano, saltos, efeito)   (cd em ticks, 20 = 1s)
    static {
        reg(Element.ICE,
                proj("Frost Shard", 25, 6, 2.2F, 1.5, fx(MobEffects.MOVEMENT_SLOWDOWN, 80, 2)),
                zone("Blizzard", 220, 2, 7, 6, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 3)),
                burst("Frozen Nova", 200, 7, 6, 40, 0.3, 0.8, fx(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)),
                pillar("Glacier Spike", 160, 7, 2.6F, fx(MobEffects.MOVEMENT_SLOWDOWN, 80, 1)));
        reg(Element.NATURE,
                proj("Thorn Dart", 25, 5, 2.0F, 1.6, fx(MobEffects.POISON, 60, 0)),
                zone("Vine Trap", 180, 2, 4.5F, 5, fx(MobEffects.MOVEMENT_SLOWDOWN, 30, 4)),
                buff("Photosynthesis", 400, 8, -1, 1, -1, 0.2F, false),
                rain("Seed Barrage", 200, 4, 5, 10, fx(MobEffects.POISON, 40, 0)));
        reg(Element.POISON,
                proj("Venom Spit", 25, 4, 2.0F, 1.5, fx(MobEffects.POISON, 100, 1)),
                zone("Toxic Cloud", 200, 1.5F, 5, 8, fx(MobEffects.POISON, 60, 1)),
                chain("Contagion", 140, 3, 4, fx(MobEffects.POISON, 100, 0)),
                burst("Plague Burst", 240, 5, 6, 0, 0.2, 0.5, fx(MobEffects.WITHER, 80, 0)));
        reg(Element.SAND,
                proj("Sand Bolt", 25, 5, 2.4F, 1.4, fx(MobEffects.BLINDNESS, 40, 0)),
                zone("Quicksand", 200, 2, 5, 6, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 4)),
                burst("Sandstorm", 220, 6, 8, 20, 0.5, 1.2, fx(MobEffects.BLINDNESS, 60, 0)),
                dash("Dune Rush", 140, 5, 12));
        reg(Element.STEAM,
                beam("Steam Jet", 30, 5, 0.7F, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)),
                zone("Scalding Fog", 200, 2.5F, 5, 6, fx(MobEffects.BLINDNESS, 40, 0)),
                burst("Boiler Burst", 200, 8, 5, 20, 0.8, 0.8, NONE),
                buff("Steam Rush", 300, 8, 2, -1, -1, 0F, false));
        reg(Element.MAGMA,
                proj("Magma Ball", 30, 7, 2.6F, 1.1, burn(4)),
                pillar("Eruption Vent", 140, 6, 3.0F, burn(5)),
                rain("Meteor Rain", 300, 6, 8, 12, burn(3)),
                zone("Lava Pool", 220, 3, 4.5F, 6, burn(3)));
        reg(Element.STORM,
                beam("Bolt Strike", 30, 7, 0.8F, NONE),
                zone("Thunderstorm", 260, 3, 8, 8, fx(MobEffects.MOVEMENT_SLOWDOWN, 20, 1)),
                burst("Gale Slam", 160, 6, 7, 20, 0.6, 1.3, NONE),
                rain("Lightning Rain", 280, 6, 8, 10, NONE));
        reg(Element.CRYSTAL,
                proj("Shard Volley", 20, 4, 1.8F, 1.7, NONE),
                pillar("Crystal Spire", 150, 7, 2.6F, NONE),
                burst("Prism Burst", 200, 7, 6, 30, 0.5, 1.0, NONE),
                buff("Crystal Armor", 400, 10, -1, -1, 1, 0F, false));
        reg(Element.SOUND,
                beam("Sonic Beam", 30, 6, 1.0F, fx(MobEffects.CONFUSION, 80, 0)),
                burst("Shockwave", 160, 6, 7, 20, 0.4, 1.4, NONE),
                zone("Bass Field", 220, 2.5F, 6, 6, fx(MobEffects.CONFUSION, 60, 0)),
                proj("Resonance Orb", 60, 9, 3.2F, 1.0, fx(MobEffects.CONFUSION, 100, 0)));
        reg(Element.SHADOW,
                dash("Shadow Step", 100, 5, 10),
                proj("Dark Bolt", 25, 6, 2.2F, 1.5, fx(MobEffects.BLINDNESS, 60, 0)),
                zone("Umbral Field", 220, 2.5F, 5.5F, 7, fx(MobEffects.MOVEMENT_SLOWDOWN, 30, 2)),
                teleport("Shade Blink", 140, 4, 25));
        reg(Element.ACID,
                proj("Acid Spit", 25, 5, 2.2F, 1.5, fx(MobEffects.WEAKNESS, 80, 0)),
                zone("Corrosive Pool", 200, 3, 4.5F, 6, fx(MobEffects.MOVEMENT_SLOWDOWN, 30, 1)),
                rain("Acid Rain", 260, 4, 7, 12, fx(MobEffects.POISON, 40, 0)),
                burst("Dissolve Nova", 220, 7, 6, 0, 0.2, 0.6, fx(MobEffects.WITHER, 100, 1)));
        reg(Element.GLASS,
                proj("Glass Shard", 20, 5, 1.8F, 1.8, NONE),
                burst("Shatter", 180, 8, 6, 20, 0.5, 1.0, NONE),
                beam("Refraction", 35, 7, 0.6F, NONE),
                buff("Glass Guard", 350, 8, -1, -1, 2, 0F, false));
        reg(Element.SAKURA,
                proj("Petal Shot", 20, 4, 2.4F, 1.5, NONE),
                rain("Petal Storm", 220, 3.5F, 7, 14, NONE),
                zone("Blossom Field", 220, 2, 5, 6, fx(MobEffects.MOVEMENT_SLOWDOWN, 30, 2)),
                buff("Spring Breeze", 350, 8, 1, 1, -1, 0.15F, false));
        reg(Element.BONE,
                proj("Bone Dart", 20, 5, 1.8F, 1.7, NONE),
                pillar("Bone Spikes", 140, 7, 2.8F, NONE),
                burst("Rib Cage", 200, 6, 5, 40, 0, 0, NONE),
                chain("Marrow Chain", 150, 4, 4, NONE));
        reg(Element.SPIRIT,
                proj("Wisp", 25, 6, 2.2F, 1.2, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)),
                dash("Phase Dash", 110, 4, 12),
                pull("Soul Vortex", 280, 2.5F, 7, 5),
                orb("Guardian Spirit", 320, 3, 8, 10));
        reg(Element.PLASMA,
                beam("Plasma Lance", 30, 8, 0.8F, burn(2)),
                proj("Plasma Orb", 50, 10, 3.4F, 1.0, NONE),
                burst("Ion Burst", 200, 8, 6.5F, 30, 0.5, 1.2, NONE),
                chain("Arc Surge", 130, 5, 5, NONE));
        reg(Element.DREAM,
                proj("Lullaby Orb", 30, 5, 2.4F, 1.2, fx(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)),
                zone("Dreamscape", 220, 1.5F, 6, 7, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 3)),
                teleport("Dream Step", 120, 3, 28),
                burst("Wake Shock", 200, 6, 6, 50, 0.3, 0.5, NONE));
        reg(Element.NIGHTMARE,
                proj("Fear Bolt", 25, 6, 2.2F, 1.5, fx(MobEffects.DARKNESS, 80, 0)),
                zone("Terror Field", 220, 2.5F, 5.5F, 7, fx(MobEffects.BLINDNESS, 60, 0)),
                pull("Abyss Maw", 300, 3, 8, 5),
                burst("Night Terror", 220, 7, 6.5F, 40, 0.3, 0.9, fx(MobEffects.WEAKNESS, 100, 0)));
        reg(Element.MIRROR,
                beam("Reflect Beam", 30, 6, 0.6F, NONE),
                chain("Echo Strike", 140, 4, 5, NONE),
                burst("Mirror Burst", 200, 7, 6, 20, 0.4, 1.0, NONE),
                teleport("Reflection Step", 110, 4, 25));
        reg(Element.RUNE,
                proj("Rune Bolt", 25, 6, 2.2F, 1.5, NONE),
                zone("Warding Circle", 220, 2.5F, 5.5F, 7, fx(MobEffects.MOVEMENT_SLOWDOWN, 30, 2)),
                rain("Glyph Rain", 240, 5, 7, 10, NONE),
                buff("Runic Power", 400, 10, -1, -1, 0, 0F, true));
        reg(Element.COMET,
                proj("Comet Dart", 20, 6, 2.2F, 2.0, NONE),
                rain("Comet Shower", 280, 6, 8, 12, NONE),
                dash("Comet Rush", 130, 6, 14),
                pillar("Impact Crater", 200, 9, 3.5F, NONE));
        reg(Element.COSMOS,
                proj("Star Bolt", 25, 8, 2.6F, 1.6, NONE),
                pull("Gravity Well", 300, 3, 8, 5),
                rain("Meteor Swarm", 300, 7, 9, 12, NONE),
                burst("Nebula Burst", 240, 10, 8, 30, 0.8, 1.4, NONE));
        reg(Element.CHAOS,
                proj("Chaos Bolt", 25, 7, 2.4F, 1.5, fx(MobEffects.CONFUSION, 80, 0)),
                rain("Entropy Rain", 260, 6, 8, 12, NONE),
                burst("Disorder", 200, 8, 7, 30, 0.6, 1.3, NONE),
                chain("Chaos Arc", 130, 5, 5, NONE));
        reg(Element.ORDER,
                beam("Decree", 30, 7, 0.7F, NONE),
                zone("Law Field", 240, 2.5F, 6, 7, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 3)),
                burst("Judgement", 220, 9, 6.5F, 40, 0.4, 0.8, NONE),
                buff("Balance", 400, 10, -1, 0, 1, 0.2F, false));
        reg(Element.LIFE,
                proj("Life Seed", 25, 4, 2.2F, 1.4, NONE),
                buff("Rejuvenate", 300, 10, -1, 2, -1, 0.3F, false),
                zone("Growth Field", 240, 2, 6, 7, fx(MobEffects.MOVEMENT_SLOWDOWN, 30, 2)),
                burst("Bloom Burst", 220, 7, 6, 0, 0.5, 1.0, NONE));
        reg(Element.DEATH,
                proj("Reaper Bolt", 25, 7, 2.2F, 1.5, fx(MobEffects.WITHER, 80, 0)),
                zone("Grave Mist", 220, 2.5F, 6, 7, fx(MobEffects.WITHER, 40, 0)),
                dash("Scythe Dash", 130, 7, 12),
                burst("Death Knell", 260, 9, 7, 40, 0.3, 0.8, fx(MobEffects.WITHER, 100, 1)));
        reg(Element.DRAGON,
                beam("Dragon Breath", 35, 8, 1.0F, burn(4)),
                dash("Wing Dive", 140, 7, 14),
                burst("Roar", 220, 8, 8, 30, 0.6, 1.3, NONE),
                rain("Dragonfire Rain", 300, 7, 8, 10, burn(3)));
        reg(Element.VOID,
                proj("Void Bolt", 25, 8, 2.6F, 1.4, NONE),
                pull("Event Horizon", 320, 3.5F, 9, 5),
                teleport("Void Step", 110, 5, 30),
                burst("Null Burst", 240, 10, 7, 30, 0.6, 1.2, NONE));
        reg(Element.CREATION,
                proj("Genesis Orb", 30, 8, 3.0F, 1.2, NONE),
                buff("Rebirth", 500, 12, -1, 2, 1, 0.4F, false),
                pillar("Spark of Creation", 180, 9, 3.4F, NONE),
                orb("Genesis Core", 350, 3.5F, 9, 12));
        reg(Element.ETERNITY,
                beam("Infinity Ray", 30, 8, 0.8F, NONE),
                zone("Stasis Field", 280, 2.5F, 7, 8, fx(MobEffects.MOVEMENT_SLOWDOWN, 60, 4)),
                pull("Endless Spiral", 320, 3.5F, 9, 6),
                burst("Eternal Pulse", 240, 10, 8, 40, 0.5, 1.2, NONE));
    }

    // ------------------------------------------------------------------ MAIS GOLPES (teclas B e N) para os 30 novos
    // Golpes com recarga >= 180 ticks sao "de segurar" (Max cast): segure a tecla para carregar.
    static {
        ext(Element.ICE, buff("Ice Armor", 400, 10, -1, -1, 1, 0F, false),
                burst("Absolute Zero", 700, 14, 10, 80, 0.4, 1.0, fx(MobEffects.MOVEMENT_SLOWDOWN, 160, 4)));
        ext(Element.NATURE, pull("Overgrowth", 500, 3, 8, 5),
                burst("World Tree Bloom", 800, 12, 9, 40, 0.6, 1.0, fx(MobEffects.POISON, 100, 1)));
        ext(Element.POISON, orb("Venom Totem", 350, 3, 8, 10),
                rain("Toxic Downpour", 600, 4, 9, 16, fx(MobEffects.WITHER, 60, 0)));
        ext(Element.SAND, pull("Sinkhole", 500, 3, 8, 5),
                burst("Desert Fury", 800, 12, 10, 40, 0.7, 1.5, fx(MobEffects.BLINDNESS, 100, 0)));
        ext(Element.STEAM, pillar("Geyser Vent", 160, 6, 3.0F, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)),
                burst("Pressure Burst", 700, 14, 9, 40, 1.0, 1.6, NONE));
        ext(Element.MAGMA, dash("Lava Surge", 150, 7, 14),
                burst("Volcano", 800, 15, 10, 40, 1.0, 1.6, burn(6)));
        ext(Element.STORM, dash("Thunder Step", 120, 6, 14),
                pull("Eye of the Storm", 600, 3.5F, 9, 6));
        ext(Element.CRYSTAL, dash("Crystal Slide", 130, 6, 13),
                rain("Crystal Rain", 600, 6, 9, 14, NONE));
        ext(Element.SOUND, dash("Sonic Boom Dash", 120, 6, 14),
                burst("Crescendo", 800, 14, 10, 50, 0.6, 1.8, fx(MobEffects.CONFUSION, 120, 0)));
        ext(Element.SHADOW, chain("Shadow Chain", 150, 5, 5, NONE),
                burst("Eclipse of Shadows", 800, 13, 9, 50, 0.4, 1.0, fx(MobEffects.BLINDNESS, 120, 0)));
        ext(Element.ACID, orb("Acid Turret", 350, 3, 8, 10),
                burst("Meltdown", 750, 14, 9, 0, 0.4, 0.8, fx(MobEffects.WITHER, 140, 1)));
        ext(Element.GLASS, chain("Shard Ricochet", 140, 5, 5, NONE),
                rain("Glass Storm", 600, 5, 9, 16, NONE));
        ext(Element.SAKURA, dash("Petal Dash", 120, 5, 13),
                burst("Hanami Blast", 800, 12, 9, 30, 0.6, 1.2, NONE));
        ext(Element.BONE, pull("Grave Pull", 450, 3, 7, 4),
                burst("Bone Prison", 700, 10, 8, 80, 0, 0, NONE));
        ext(Element.SPIRIT, teleport("Spirit Step", 110, 4, 28),
                burst("Haunting", 750, 12, 9, 60, 0.4, 1.0, fx(MobEffects.WEAKNESS, 120, 0)));
        ext(Element.PLASMA, pull("Plasma Vortex", 500, 4, 8, 5),
                burst("Overload", 800, 16, 10, 40, 0.8, 1.6, burn(3)));
        ext(Element.DREAM, pull("Sleepwalk", 450, 2, 8, 5),
                rain("Dream Shower", 600, 4, 9, 16, fx(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)));
        ext(Element.NIGHTMARE, dash("Night Stalk", 130, 6, 13),
                rain("Dread Rain", 600, 5, 9, 14, fx(MobEffects.DARKNESS, 80, 0)));
        ext(Element.MIRROR, orb("Mirror Image", 350, 3.5F, 9, 10),
                burst("Shatter Realm", 800, 14, 10, 40, 0.6, 1.4, NONE));
        ext(Element.RUNE, chain("Rune Chain", 140, 5, 5, NONE),
                burst("Seal of Power", 800, 13, 9, 40, 0.5, 1.2, NONE));
        ext(Element.COMET, teleport("Comet Jump", 110, 5, 30),
                burst("Cometfall", 800, 16, 10, 40, 1.0, 1.6, NONE));
        ext(Element.COSMOS, orb("Star Sentinel", 350, 4, 9, 12),
                burst("Big Bang", 900, 18, 11, 40, 1.0, 2.0, NONE));
        ext(Element.CHAOS, pull("Entropy Pull", 450, 3, 8, 5),
                burst("Pandemonium", 800, 15, 10, 40, 0.8, 1.6, fx(MobEffects.CONFUSION, 120, 0)));
        ext(Element.ORDER, orb("Order Guardian", 350, 3.5F, 9, 12),
                rain("Edict Rain", 600, 6, 9, 12, NONE));
        ext(Element.LIFE, orb("Life Totem", 350, 2, 8, 12),
                burst("Overgrowth Wave", 800, 10, 10, 30, 0.6, 1.2, NONE));
        ext(Element.DEATH, chain("Soul Chain", 140, 5, 5, fx(MobEffects.WITHER, 80, 0)),
                pull("Grave Maw", 500, 3.5F, 8, 5));
        ext(Element.DRAGON, pillar("Dragonfire Pillar", 180, 9, 3.4F, burn(5)),
                burst("Ancient Roar", 900, 16, 11, 50, 1.0, 1.8, burn(4)));
        ext(Element.VOID, orb("Void Eye", 350, 4, 9, 10),
                rain("Null Rain", 650, 6, 9, 12, NONE));
        ext(Element.CREATION, zone("Radiant Field", 300, 3, 7, 8, fx(MobEffects.MOVEMENT_SLOWDOWN, 30, 2)),
                burst("Big Creation", 900, 16, 11, 40, 0.8, 1.6, NONE));
        ext(Element.ETERNITY, orb("Eternal Sentinel", 350, 4, 9, 12),
                burst("End of Time", 900, 18, 11, 60, 0.6, 1.8, fx(MobEffects.MOVEMENT_SLOWDOWN, 200, 5)));
    }

    // ------------------------------------------------------------------ 6 ELEMENTOS NOVOS (6 golpes cada: Z X C V B N)
    static {
        reg(Element.SOLAR,
                proj("Sunbolt", 25, 7, 2.4F, 1.5, burn(3)),
                beam("Solar Ray", 30, 8, 0.9F, burn(3)),
                burst("Flare", 200, 8, 6.5F, 30, 0.6, 1.2, NONE),
                zone("Scorching Field", 220, 3, 6, 7, burn(2)),
                pull("Miniature Sun", 500, 4, 8, 5),
                burst("Solar Eclipse Burst", 900, 18, 11, 50, 1.0, 1.8, burn(5)));
        reg(Element.LUNAR,
                proj("Moonbeam Orb", 25, 6, 2.2F, 1.4, fx(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)),
                beam("Lunar Lance", 30, 7, 0.7F, NONE),
                zone("Moonlit Field", 230, 2.5F, 6, 7, fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 2)),
                teleport("Moon Step", 110, 4, 28),
                rain("Crescent Rain", 500, 6, 9, 12, NONE),
                burst("Full Moon", 900, 16, 10, 40, 0.8, 1.6, NONE));
        reg(Element.GOLD,
                proj("Gold Bullet", 20, 8, 2.0F, 1.9, NONE),
                pillar("Gilded Spire", 160, 8, 2.8F, NONE),
                burst("Gold Rush", 200, 8, 6, 30, 0.5, 1.1, NONE),
                buff("Golden Skin", 400, 10, -1, -1, 2, 0F, false),
                rain("Coin Storm", 500, 5, 9, 16, NONE),
                burst("Midas Touch", 900, 17, 10, 60, 0.4, 1.0, fx(MobEffects.MOVEMENT_SLOWDOWN, 200, 5)));
        reg(Element.WEB,
                proj("Web Shot", 25, 3, 2.4F, 1.5, fx(MobEffects.MOVEMENT_SLOWDOWN, 100, 4)),
                zone("Sticky Web", 200, 1.5F, 6, 8, fx(MobEffects.MOVEMENT_SLOWDOWN, 60, 5)),
                chain("Silk Thread", 140, 3, 5, fx(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)),
                dash("Web Swing", 120, 4, 13),
                pull("Spider Nest", 500, 3, 8, 5),
                burst("Cocoon", 800, 8, 8, 100, 0, 0, NONE));
        reg(Element.RUST,
                proj("Rust Bolt", 25, 5, 2.2F, 1.4, fx(MobEffects.WEAKNESS, 80, 0)),
                zone("Corrosion Field", 220, 2.5F, 5.5F, 7, fx(MobEffects.WEAKNESS, 60, 1)),
                burst("Oxidize", 200, 7, 6, 20, 0.3, 0.8, fx(MobEffects.WITHER, 60, 0)),
                dash("Scrap Rush", 130, 6, 12),
                rain("Scrap Rain", 550, 5, 9, 14, NONE),
                burst("Decay Wave", 850, 14, 10, 40, 0.5, 1.3, fx(MobEffects.WITHER, 160, 1)));
        reg(Element.ECHO,
                beam("Echo Ping", 30, 6, 0.8F, NONE),
                chain("Reverberation", 130, 4, 6, NONE),
                burst("Resonant Blast", 200, 7, 6.5F, 30, 0.5, 1.2, fx(MobEffects.CONFUSION, 80, 0)),
                teleport("Echo Step", 110, 4, 28),
                orb("Echo Clone", 350, 3.5F, 9, 10),
                burst("Infinite Echo", 900, 17, 11, 50, 0.8, 1.6, fx(MobEffects.CONFUSION, 160, 0)));
    }
}
