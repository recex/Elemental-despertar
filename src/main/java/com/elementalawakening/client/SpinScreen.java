package com.elementalawakening.client;

import com.elementalawakening.element.Element;
import com.elementalawakening.network.ModNetwork;
import com.elementalawakening.network.RequestSpinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class SpinScreen extends Screen {
    private static final int ITEM_W = 90;
    private static final int ITEM_H = 56;
    private static final int GAP = 6;
    private static final int STEP = ITEM_W + GAP;
    private static final int STRIP_SIZE = 60;
    private static final int TARGET_INDEX = 50;
    private static final long SPIN_MS = 5000L;

    private final List<Element> strip = new ArrayList<>();
    private final RandomSource random = RandomSource.create();

    private Button spinButton;
    private double offset = ITEM_W / 2.0;
    private double startOffset;
    private double targetOffset;
    private long startTime;
    private int lastTickIndex = -1;

    private boolean waiting;   // esperando o servidor responder
    private boolean spinning;  // animacao rodando
    private Element result;

    public SpinScreen() {
        super(Component.literal("Despertar Elemental"));
        buildStrip(null);
    }

    private void buildStrip(Element target) {
        strip.clear();
        for (int i = 0; i < STRIP_SIZE; i++) {
            strip.add(Element.roll(random));
        }
        if (target != null) {
            strip.set(TARGET_INDEX, target);
        }
    }

    @Override
    protected void init() {
        spinButton = Button.builder(Component.literal("Girar"), b -> onSpinPressed())
                .bounds(this.width / 2 - 50, this.height / 2 + 50, 100, 20)
                .build();
        addRenderableWidget(spinButton);
        spinButton.active = !(waiting || spinning);
    }

    private void onSpinPressed() {
        if (waiting || spinning) return;
        waiting = true;
        result = null;
        spinButton.active = false;
        ModNetwork.CHANNEL.sendToServer(new RequestSpinPacket());
    }

    /** Chamado quando o servidor devolve o resultado. */
    public void startSpin(Element element) {
        waiting = false;
        result = element;
        buildStrip(element);
        startOffset = ITEM_W / 2.0;
        offset = startOffset;
        targetOffset = TARGET_INDEX * STEP + ITEM_W / 2.0 + (random.nextDouble() - 0.5) * ITEM_W * 0.6;
        startTime = System.currentTimeMillis();
        lastTickIndex = -1;
        spinning = true;
    }

    private void playTick() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4F));
    }

    private void updateAnimation() {
        if (!spinning) return;

        double t = Mth.clamp((System.currentTimeMillis() - startTime) / (double) SPIN_MS, 0.0, 1.0);
        double eased = 1.0 - Math.pow(1.0 - t, 4); // desacelera no final
        offset = startOffset + (targetOffset - startOffset) * eased;

        int idx = Mth.floor(offset / STEP);
        if (idx != lastTickIndex) {
            lastTickIndex = idx;
            playTick();
        }

        if (t >= 1.0) {
            spinning = false;
            spinButton.active = true;
            spinButton.setMessage(Component.literal("Girar de novo"));

            boolean rare = result != null
                    && result.rarity.ordinal() >= Element.Rarity.EXOTIC.ordinal();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    rare ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 1.0F));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        updateAnimation();

        int cx = this.width / 2;
        int cy = this.height / 2 - 20;
        int viewW = Math.min(this.width - 40, 420);
        int x1 = cx - viewW / 2;
        int x2 = cx + viewW / 2;
        int y1 = cy - ITEM_H / 2 - 6;
        int y2 = cy + ITEM_H / 2 + 6;

        g.drawCenteredString(this.font, this.title, cx, y1 - 26, 0xFFFFFF);

        // moldura + fundo
        g.fill(x1 - 2, y1 - 2, x2 + 2, y2 + 2, 0xFF000000);
        g.fill(x1, y1, x2, y2, 0xFF1B1B1B);

        g.enableScissor(x1, y1, x2, y2);
        int top = cy - ITEM_H / 2;
        for (int i = 0; i < strip.size(); i++) {
            int left = (int) (cx - offset + i * STEP);
            if (left + ITEM_W < x1 || left > x2) continue;

            Element el = strip.get(i);
            g.fill(left, top, left + ITEM_W, top + ITEM_H, 0xFF000000 | el.color);
            g.fill(left + 2, top + 2, left + ITEM_W - 2, top + ITEM_H - 2, 0xFF2A2A2A);
            g.drawCenteredString(this.font, el.displayName, left + ITEM_W / 2, top + ITEM_H / 2 - 10,
                    0xFF000000 | el.color);
            g.drawCenteredString(this.font, el.rarity.displayName, left + ITEM_W / 2, top + ITEM_H / 2 + 4,
                    0xFF000000 | el.rarity.color);
        }
        g.disableScissor();

        // marcador central
        g.fill(cx - 1, y1 - 8, cx + 1, y2 + 8, 0xFFFFFFFF);

        // resultado
        if (!spinning && !waiting && result != null) {
            g.drawCenteredString(this.font,
                    Component.literal("Você despertou: " + result.displayName + " (" + result.rarity.displayName + ")"),
                    cx, y2 + 14, 0xFF000000 | result.color);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
