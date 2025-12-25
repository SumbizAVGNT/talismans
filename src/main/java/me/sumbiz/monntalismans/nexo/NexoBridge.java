package me.sumbiz.monntalismans.nexo;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

public final class NexoBridge {

    private final Logger log;

    private Method mExists;
    private Method mItemFromId;
    private Method mIdFromItem;

    public NexoBridge(Logger log) {
        this.log = log;
    }

    public boolean init() {
        List<String> candidates = List.of(
                "com.nexomc.nexo.api.NexoItems",
                "com.nexomc.nexo.api.items.NexoItems",
                "com.nexomc.nexo.items.NexoItems"
        );

        for (String cn : candidates) {
            try {
                Class<?> c = Class.forName(cn);
                Method exists = findStatic(c, "exists", String.class);
                Method itemFromId = findStatic(c, "itemFromId", String.class);
                Method idFromItem = findStatic(c, "idFromItem", ItemStack.class);

                if (exists != null && itemFromId != null && idFromItem != null) {
                    this.mExists = exists;
                    this.mItemFromId = itemFromId;
                    this.mIdFromItem = idFromItem;

                    log.info("NexoBridge: hooked " + cn);
                    return true;
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                log.warning("NexoBridge: fail on " + cn + " -> " + t.getMessage());
            }
        }

        log.severe("NexoBridge: не нашёл NexoItems API (exists/itemFromId/idFromItem).");
        return false;
    }

    public boolean exists(String id) {
        try {
            return (boolean) mExists.invoke(null, id);
        } catch (Throwable t) {
            return false;
        }
    }

    public ItemStack itemFromId(String id) {
        try {
            Object o = mItemFromId.invoke(null, id);
            if (o == null) return null;
            if (o instanceof ItemStack is) return is;

            // Иногда API возвращает ItemBuilder — пробуем build()/toItemStack()
            try {
                Method build = o.getClass().getMethod("build");
                Object built = build.invoke(o);
                if (built instanceof ItemStack is2) return is2;
            } catch (NoSuchMethodException ignored) {}

            try {
                Method toItemStack = o.getClass().getMethod("toItemStack");
                Object built = toItemStack.invoke(o);
                if (built instanceof ItemStack is2) return is2;
            } catch (NoSuchMethodException ignored) {}

        } catch (Throwable ignored) {}

        return null;
    }

    public String idFromItem(ItemStack item) {
        if (item == null) return null;
        try {
            Object o = mIdFromItem.invoke(null, item);
            return (o == null) ? null : String.valueOf(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method findStatic(Class<?> c, String name, Class<?>... params) {
        try {
            Method m = c.getMethod(name, params);
            if ((m.getModifiers() & java.lang.reflect.Modifier.STATIC) == 0) return null;
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
