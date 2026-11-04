package com.elementalawakening.command;

import com.elementalawakening.ElementalMod;
import com.elementalawakening.ability.AbilityUtil;
import com.elementalawakening.element.Element;
import com.elementalawakening.element.ElementStorage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /aura  (ou /elemento):
 *   /aura set <elemento>   escolhe seu elemento na hora (aceita nome em portugues ou ingles)
 *   /aura girar            sorteia um elemento (como o Orbe)
 *   /aura reset            limpa seu elemento
 *   /aura lista            mostra todos os elementos
 *   /aura ver              mostra o seu elemento atual
 */
@Mod.EventBusSubscriber(modid = ElementalMod.MODID)
public class ModCommands {
    private static final SuggestionProvider<CommandSourceStack> NAMES = (ctx, builder) -> {
        List<String> list = new ArrayList<>();
        for (Element e : Element.values()) list.add(norm(e.displayName));
        return SharedSuggestionProvider.suggest(list, builder);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(build("aura"));
        event.getDispatcher().register(build("elemento"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String root) {
        return Commands.literal(root)
                .then(Commands.literal("set")
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .suggests(NAMES)
                                .executes(ctx -> set(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "nome")))))
                .then(Commands.literal("girar")
                        .executes(ctx -> spin(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("reset")
                        .executes(ctx -> reset(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("lista")
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("ver")
                        .executes(ctx -> show(ctx.getSource().getPlayerOrException())));
    }

    private static String norm(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static Element find(String input) {
        String q = norm(input);
        for (Element e : Element.values()) {
            if (e.name().toLowerCase(Locale.ROOT).equals(q) || norm(e.displayName).equals(q)) return e;
        }
        switch (q) {
            case "colapso", "reality", "collapse" -> { return Element.REALITY_COLLAPSE; }
            case "trevas" -> { return Element.DARKNESS; }
            default -> { return null; }
        }
    }

    private static int set(ServerPlayer p, String name) {
        Element e = find(name);
        if (e == null) {
            p.sendSystemMessage(Component.literal("Elemento não encontrado. Use /aura lista."));
            return 0;
        }
        apply(p, e);
        p.sendSystemMessage(Component.literal("Seu elemento agora é: " + e.displayName + " (" + e.rarity.displayName + ")"));
        return 1;
    }

    private static int spin(ServerPlayer p) {
        Element e = Element.roll(p.getRandom());
        apply(p, e);
        p.sendSystemMessage(Component.literal("Você despertou: " + e.displayName + " (" + e.rarity.displayName + ")"));
        return 1;
    }

    private static void apply(ServerPlayer p, Element e) {
        ElementStorage.set(p, e);
        AbilityUtil.circle(p.serverLevel(), p.position(), e, 4.0F, 60);
    }

    private static int reset(ServerPlayer p) {
        ElementStorage.clear(p);
        p.sendSystemMessage(Component.literal("Elemento removido."));
        return 1;
    }

    private static int show(ServerPlayer p) {
        Element e = ElementStorage.get(p);
        p.sendSystemMessage(Component.literal(e == null ? "Você ainda não tem elemento."
                : "Seu elemento: " + e.displayName + " (" + e.rarity.displayName + ")"));
        return 1;
    }

    private static int list(CommandSourceStack src) {
        StringBuilder sb = new StringBuilder("Elementos: ");
        for (Element e : Element.values()) sb.append(norm(e.displayName)).append(", ");
        String text = sb.substring(0, sb.length() - 2);
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
