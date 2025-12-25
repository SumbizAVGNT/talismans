package me.sumbiz.monntalismans.nexo;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

public final class NexoEventBridge {

    private NexoEventBridge() {}

    public static void tryRegister(Plugin plugin, Logger log, Runnable onReloadLikeEvent) {
        List<String> candidates = List.of(
                "com.nexomc.nexo.api.events.NexoItemsLoadedEvent",
                "com.nexomc.nexo.api.events.items.NexoItemsLoadedEvent",
                "com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent",
                "com.nexomc.nexo.api.events.mechanics.NexoMechanicsRegisteredEvent",
                "com.nexomc.nexo.api.events.resourcepack.NexoPackUploadEvent"
        );

        for (String cn : candidates) {
            Class<? extends Event> eventClass = loadEventClass(cn);
            if (eventClass == null) continue;

            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    new Listener() {},
                    EventPriority.MONITOR,
                    (listener, event) -> {
                        try { onReloadLikeEvent.run(); } catch (Throwable ignored) {}
                    },
                    plugin,
                    true
            );

            log.info("NexoEventBridge: listening " + cn);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Event> loadEventClass(String cn) {
        try {
            Class<?> c = Class.forName(cn);
            if (!Event.class.isAssignableFrom(c)) return null;
            return (Class<? extends Event>) c;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
