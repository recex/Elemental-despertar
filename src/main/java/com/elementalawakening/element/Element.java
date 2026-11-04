package com.elementalawakening.element;

import net.minecraft.util.RandomSource;

public enum Element {
    FIRE("Fogo", Rarity.COMMON, 29.95, 0xFF5A1F),
    WATER("Água", Rarity.COMMON, 29.95, 0x3FA9FF),
    LIGHTNING("Raio", Rarity.COMMON, 29.95, 0xA8D8FF),
    EARTH("Terra", Rarity.UNCOMMON, 3.75, 0xC9A66B),
    WIND("Vento", Rarity.UNCOMMON, 3.75, 0xEFFFFF),
    LIGHT("Luz", Rarity.RARE, 0.7, 0xFFFFC8),
    DARKNESS("Escuridão", Rarity.RARE, 0.7, 0x9A55D8),   // fonte conflitante: Rare ou Legendary
    METAL("Metal", Rarity.RARE, 0.7, 0xB0B7C3),
    ECLIPSE("Eclipse", Rarity.EXOTIC, 0.4, 0xF0F0FF),
    BLOOD("Sangue", Rarity.EXOTIC, 0.4, 0xE0203A),
    CELESTIAL("Celestial", Rarity.LEGENDARY, 0.15, 0xFFC090),
    GRAVITY("Gravidade", Rarity.CURSED_LEGENDARY, 0.1, 0xD060FF),
    TIME("Tempo", Rarity.HEAVENLY, 0.01, 0xF0F8FF),
    REALITY_COLLAPSE("Colapso Real", Rarity.HEAVENLY, 0.01, 0xE6E6FF),

    // ---------------- elementos novos (criados para o mod) ----------------
    ICE("Gelo", Rarity.UNCOMMON, 2.5, 0xA8E8FF),
    NATURE("Natureza", Rarity.UNCOMMON, 2.5, 0x5EDB6A),
    POISON("Veneno", Rarity.UNCOMMON, 2.5, 0x9BE03A),
    SAND("Areia", Rarity.UNCOMMON, 2.5, 0xE6C27A),
    STEAM("Vapor", Rarity.UNCOMMON, 2.5, 0xDDE8EE),
    MAGMA("Magma", Rarity.RARE, 0.5, 0xFF6B1A),
    STORM("Tempestade", Rarity.RARE, 0.5, 0x6C8CFF),
    CRYSTAL("Cristal", Rarity.RARE, 0.5, 0xB48CFF),
    SOUND("Som", Rarity.RARE, 0.5, 0xFF9EDC),
    SHADOW("Sombra", Rarity.RARE, 0.5, 0x7A56C8),
    ACID("Ácido", Rarity.RARE, 0.5, 0xC8FF2E),
    GLASS("Vidro", Rarity.RARE, 0.5, 0xBFF4FF),
    SAKURA("Sakura", Rarity.RARE, 0.5, 0xFFA6C9),
    BONE("Osso", Rarity.EXOTIC, 0.25, 0xEDE6D0),
    SPIRIT("Espírito", Rarity.EXOTIC, 0.25, 0x8CFFE0),
    PLASMA("Plasma", Rarity.EXOTIC, 0.25, 0xFF4DFF),
    DREAM("Sonho", Rarity.EXOTIC, 0.25, 0xFFB6F0),
    NIGHTMARE("Pesadelo", Rarity.EXOTIC, 0.25, 0xC0306A),
    MIRROR("Espelho", Rarity.EXOTIC, 0.25, 0xE0F0FF),
    RUNE("Runa", Rarity.EXOTIC, 0.25, 0xFFD060),
    COMET("Cometa", Rarity.EXOTIC, 0.25, 0x80D8FF),
    COSMOS("Cosmos", Rarity.LEGENDARY, 0.1, 0x6A5CFF),
    CHAOS("Caos", Rarity.LEGENDARY, 0.1, 0xFF5050),
    ORDER("Ordem", Rarity.LEGENDARY, 0.1, 0xFFE070),
    LIFE("Vida", Rarity.LEGENDARY, 0.1, 0x7CFF9A),
    DEATH("Morte", Rarity.LEGENDARY, 0.1, 0xB8B8D8),
    DRAGON("Dragão", Rarity.LEGENDARY, 0.1, 0xFF8A3C),
    VOID("Vazio", Rarity.CURSED_LEGENDARY, 0.06, 0xA070FF),
    CREATION("Criação", Rarity.HEAVENLY, 0.008, 0xFFF2A0),
    ETERNITY("Eternidade", Rarity.HEAVENLY, 0.008, 0xA0FFF0),

    // ---------------- mais elementos novos ----------------
    SOLAR("Sol", Rarity.LEGENDARY, 0.1, 0xFFC93C),
    LUNAR("Lua", Rarity.LEGENDARY, 0.1, 0xBFD0FF),
    GOLD("Ouro", Rarity.EXOTIC, 0.25, 0xFFD54A),
    WEB("Teia", Rarity.RARE, 0.5, 0xE8E8F0),
    RUST("Ferrugem", Rarity.UNCOMMON, 2.5, 0xB5651D),
    ECHO("Eco", Rarity.EXOTIC, 0.25, 0x9EE8FF);

    // A ordem importa: usada para comparar raridade (ordinal)
    public enum Rarity {
        COMMON("Comum", 0xAAAAAA),
        UNCOMMON("Incomum", 0x55FF55),
        RARE("Raro", 0x5599FF),
        EXOTIC("Exótico", 0xFF5555),
        LEGENDARY("Lendário", 0xFFAA00),
        CURSED_LEGENDARY("Amaldiçoado", 0xAA00AA),
        HEAVENLY("Divino", 0xFF55FF);

        public final String displayName;
        public final int color;

        Rarity(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    public final String displayName;
    public final Rarity rarity;
    public final double weight; // peso relativo (nao precisa somar 100)
    public final int color;     // RGB

    Element(String displayName, Rarity rarity, double weight, int color) {
        this.displayName = displayName;
        this.rarity = rarity;
        this.weight = weight;
        this.color = color;
    }

    public static Element byId(int id) {
        Element[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    public static Element roll(RandomSource random) {
        double total = 0;
        for (Element e : values()) total += e.weight;
        double x = random.nextDouble() * total;
        for (Element e : values()) {
            x -= e.weight;
            if (x < 0) return e;
        }
        return FIRE;
    }
}
