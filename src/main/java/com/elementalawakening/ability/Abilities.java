package com.elementalawakening.ability;

import com.elementalawakening.element.Element;
import com.elementalawakening.element.ElementStorage;
import com.elementalawakening.entity.FxEntity;
import com.elementalawakening.entity.MagicCircleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Abilities {
    private static final Map<Element, AbilitySet> SETS = new EnumMap<>(Element.class);

    /** Quem esta carregando um golpe agora (slot e tick de inicio). Validado no servidor. */
    private record ChargeState(int slot, long start) {}

    private static final Map<UUID, ChargeState> CHARGING = new HashMap<>();
    private static final Map<UUID, Long> LAST_CAST = new HashMap<>();

    private static final int MAX_HOLD_TICKS = 80;   // solta sozinho (evita tecla "presa")
    private static final int FULL_CHARGE_TICKS = 56; // tempo ate a carga maxima

    static {
        SETS.put(Element.FIRE, FireAbilities.SET);
        SETS.put(Element.WATER, WaterAbilities.SET);
        SETS.put(Element.LIGHTNING, LightningAbilities.SET);
        SETS.put(Element.EARTH, EarthAbilities.SET);
        SETS.put(Element.WIND, WindAbilities.SET);
        SETS.put(Element.LIGHT, LightAbilities.SET);
        SETS.put(Element.DARKNESS, DarknessAbilities.SET);
        SETS.put(Element.METAL, MetalAbilities.SET);
        SETS.put(Element.ECLIPSE, EclipseAbilities.SET);
        SETS.put(Element.BLOOD, BloodAbilities.SET);
        SETS.put(Element.CELESTIAL, CelestialAbilities.SET);
        SETS.put(Element.GRAVITY, GravityAbilities.SET);
        SETS.put(Element.TIME, TimeAbilities.SET);
        SETS.put(Element.REALITY_COLLAPSE, RealityAbilities.SET);
        SETS.putAll(GenAbilities.all());
    }

    private Abilities() {}

    public static AbilitySet.Entry entryOf(Element el, int slot) {
        AbilitySet set = el == null ? null : SETS.get(el);
        return set == null ? null : set.get(slot);
    }

    public static String nameOf(Element el, int slot) {
        AbilitySet.Entry e = entryOf(el, slot);
        return e == null ? null : e.name();
    }

    /** Limpa o estado de um jogador (logout). */
    public static void forget(UUID id) {
        CHARGING.remove(id);
        LAST_CAST.remove(id);
        Cooldowns.clear(id);
    }

    // ------------------------------------------------------------------ aperto da tecla
    public static void onPress(ServerPlayer player, int slot) {
        if (!player.isAlive() || player.isSpectator()) return;
        Element element = ElementStorage.get(player);
        if (element == null) {
            AbilityUtil.actionbar(player, "Você ainda não tem elemento. Use o Orbe do Despertar.");
            return;
        }
        AbilitySet.Entry entry = entryOf(element, slot);
        if (entry == null) {
            AbilityUtil.actionbar(player, "Sem habilidade nessa tecla.");
            return;
        }
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        Long last = LAST_CAST.get(id);
        if (last != null && now - last < 3) return;      // anti-spam de pacotes
        if (CHARGING.containsKey(id)) return;            // ja esta carregando outro golpe

        long left = Cooldowns.remaining(player, slot);
        if (left > 0) {
            AbilityUtil.actionbar(player, entry.name() + " em recarga: " + String.format("%.1f", left / 20.0) + "s");
            return;
        }
        if (!entry.charge()) {
            castNow(player, element, slot, entry, 0.0F);
            return;
        }
        CHARGING.put(id, new ChargeState(slot, now));
        startChargeVisual(player, element);
        chargeTick(player, element, entry.name(), slot, now, 0);
    }

    // ------------------------------------------------------------------ soltura da tecla
    public static void onRelease(ServerPlayer player, int slot) {
        ChargeState st = CHARGING.get(player.getUUID());
        if (st == null || st.slot() != slot) return;
        CHARGING.remove(player.getUUID());
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        Element element = ElementStorage.get(player);
        if (element == null || !player.isAlive()) return;
        AbilitySet.Entry entry = entryOf(element, slot);
        if (entry == null) return;

        long held = player.level().getGameTime() - st.start();
        float charge = Mth.clamp((held - 6) / (float) (FULL_CHARGE_TICKS - 6), 0.0F, 1.0F);
        castNow(player, element, slot, entry, charge);
    }

    // ------------------------------------------------------------------ execucao
    private static void castNow(ServerPlayer player, Element element, int slot, AbilitySet.Entry entry, float charge) {
        if (Cooldowns.remaining(player, slot) > 0) return;

        boolean ok;
        Charge.begin(player, charge);
        try {
            ok = entry.action().test(player);
        } finally {
            Charge.end();
        }
        if (!ok) {
            Charge.begin(player, 0.0F);
            Charge.end();
            return;
        }
        // golpe carregado demora mais para recarregar (balanceamento)
        Cooldowns.set(player, slot, (int) (entry.cooldown() * (1.0F + 0.5F * charge)));
        LAST_CAST.put(player.getUUID(), player.level().getGameTime());
        castEffects(player, element, charge);
        StateSync.send(player);
    }

    /** Animacao de conjuracao: o braco balanca e um circulo magico em pe se abre diante do personagem. */
    private static void castEffects(ServerPlayer player, Element element, float charge) {
        player.swing(InteractionHand.MAIN_HAND, true);
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.getEyePosition().add(look.scale(1.5)).add(0, -0.3, 0);
        MagicCircleEntity.spawnVertical(player.serverLevel(), pos, element, element.color,
                0.85F + 0.6F * charge, 14, player.getYRot());
    }

    // ------------------------------------------------------------------ visual da carga
    private static void startChargeVisual(ServerPlayer p, Element el) {
        ServerLevel level = p.serverLevel();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MAX_HOLD_TICKS + 20, 1, false, false));
        AbilityUtil.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 0.8F, 0.6F);
        Vec3 g = p.position();
        // anel que cresce no chao + esfera de energia no peito (as duas crescem durante a carga)
        new FxEntity.Spec(FxEntity.RING).pos(g.add(0, 0.15, 0)).color(el.color).r(2.4F).h(0.07F)
                .life(FULL_CHARGE_TICKS + 4).style(FxEntity.SPIN).grow(FxEntity.SLOW).spawn(level);
        new FxEntity.Spec(FxEntity.SPHERE).pos(g.add(0, 1.1, 0)).color(el.color).r(0.8F)
                .life(FULL_CHARGE_TICKS + 4).style(FxEntity.PULSE).grow(FxEntity.SLOW).spawn(level);
    }

    private static void chargeTick(ServerPlayer p, Element el, String name, int slot, long start, int i) {
        ChargeState st = CHARGING.get(p.getUUID());
        if (st == null || st.slot() != slot || st.start() != start) return; // soltou ou cancelou
        if (!p.isAlive() || p.isSpectator()) {
            CHARGING.remove(p.getUUID());
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            return;
        }
        long held = p.level().getGameTime() - start;
        if (held >= MAX_HOLD_TICKS) {
            onRelease(p, slot);
            return;
        }
        float charge = Mth.clamp((held - 6) / (float) (FULL_CHARGE_TICKS - 6), 0.0F, 1.0F);
        int filled = Math.round(charge * 10);
        AbilityUtil.actionbar(p, name + "  " + "▮".repeat(filled) + "▯".repeat(10 - filled) + (charge >= 1.0F ? "  MAX!" : ""));

        ServerLevel level = p.serverLevel();
        Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(1.0)).add(0, -0.4, 0);
        double spread = 0.35 - 0.2 * charge;
        level.sendParticles(ElementFx.p1(el), hand.x, hand.y, hand.z, 3 + (int) (charge * 8), spread, spread, spread, 0.03);
        if (i % 5 == 0) {
            AbilityUtil.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 0.6F + charge * 1.4F);
        }
        if (held >= FULL_CHARGE_TICKS && held < FULL_CHARGE_TICKS + 2) { // chegou na carga maxima: aviso
            AbilityUtil.sound(level, p.position(), SoundEvents.PLAYER_LEVELUP, 0.8F, 1.4F);
            ElementFx.burst(level, el, p.position(), 2.5F);
        }
        Scheduler.run(2, () -> chargeTick(p, el, name, slot, start, i + 1));
    }
}
