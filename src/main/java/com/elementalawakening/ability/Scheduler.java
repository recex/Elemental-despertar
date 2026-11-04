package com.elementalawakening.ability;

import com.elementalawakening.ElementalMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

/** Agenda coisas para daqui a N ticks (20 ticks = 1 segundo). So roda na thread do servidor. */
@Mod.EventBusSubscriber(modid = ElementalMod.MODID)
public final class Scheduler {
    private static final class Task {
        int delay;
        final Runnable action;

        Task(int delay, Runnable action) {
            this.delay = delay;
            this.action = action;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private Scheduler() {}

    public static void run(int delayTicks, Runnable action) {
        PENDING.add(new Task(delayTicks, action));
    }

    /** Executa action(i) para i = 0..times-1, com 'interval' ticks entre cada uma. */
    public static void repeat(int times, int interval, IntConsumer action) {
        for (int i = 0; i < times; i++) {
            final int index = i;
            run(i * interval, () -> action.accept(index));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        TASKS.addAll(PENDING);
        PENDING.clear();

        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (--t.delay <= 0) {
                it.remove();
                try {
                    t.action.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
