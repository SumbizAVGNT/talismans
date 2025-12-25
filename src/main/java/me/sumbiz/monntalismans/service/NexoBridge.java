package me.sumbiz.monntalismans.service;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public final class NexoBridge {

    private final Plugin plugin;
    private final boolean available;

    private Class<?> nexoItemsClz;
    private Method mExists;
    private Method mItemFromId;
    private Method mBuild;

    public NexoBridge(Plugin plugin) {
        this.plugin = plugin;
        this.available = init();
    }

    public boolean isAvailable() {
        return available;
    }

    public ItemStack buildItem(String nexoId) {
        if (!available) return null;
        try {
            Boolean exists = (Boolean) mExists.invoke(null, nexoId);
            if (exists == null || !exists) return null;

            Object builder = mItemFromId.invoke(null, nexoId);
            if (builder == null) return null;

            return (ItemStack) mBuild.invoke(builder);
        } catch (Throwable t) {
            plugin.getLogger().warning("[NexoBridge] buildItem failed for " + nexoId + ": " + t.getMessage());
            return null;
        }
    }

    private boolean init() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Nexo") == null) return false;

            nexoItemsClz = Class.forName("com.nexomc.nexo.api.NexoItems");
            mExists = nexoItemsClz.getMethod("exists", String.class);
            mItemFromId = nexoItemsClz.getMethod("itemFromId", String.class);

            // builder.build()
            // тип билдера нам не важен — метод ищем по имени
            // (возвращает ItemStack)
            Class<?> builderClz = Class.forName("com.nexomc.nexo.items.ItemBuilder");
            mBuild = builderClz.getMethod("build");

            plugin.getLogger().info("[NexoBridge] Nexo detected, template-from-nexo enabled.");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("[NexoBridge] Nexo not usable: " + t.getMessage());
            return false;
        }
    }
}
