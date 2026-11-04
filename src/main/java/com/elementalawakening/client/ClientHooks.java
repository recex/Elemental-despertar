package com.elementalawakening.client;

import com.elementalawakening.element.Element;
import net.minecraft.client.Minecraft;

/** Tudo que mexe em classes so do cliente fica aqui. */
public class ClientHooks {
    public static void openSpinScreen() {
        Minecraft.getInstance().setScreen(new SpinScreen());
    }

    public static void onSpinResult(int elementId) {
        if (Minecraft.getInstance().screen instanceof SpinScreen screen) {
            screen.startSpin(Element.byId(elementId));
        }
    }
}
