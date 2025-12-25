package me.sumbiz.moontalismans;

import me.sumbiz.moontalismans.listeners.MenuListener;
import me.sumbiz.moontalismans.mechanics.EffectManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MoonTalismansPlugin extends JavaPlugin {
    private ItemManager itemManager;
    private EffectManager effectManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize PDC keys for items
        TalismanItem.initKeys(this);

        // Load items
        itemManager = new ItemManager(this);
        itemManager.reload();

        // Initialize effect system
        effectManager = new EffectManager(this);
        Bukkit.getPluginManager().registerEvents(effectManager, this);

        // Register menu listener
        MenuListener menuListener = new MenuListener(this);
        Bukkit.getPluginManager().registerEvents(menuListener, this);

        // Register commands
        PluginCommand cmd = getCommand("talismans");
        if (cmd != null) {
            TalismansCommand commandHandler = new TalismansCommand(this);
            cmd.setExecutor(commandHandler);
            cmd.setTabCompleter(commandHandler);
        }

        getLogger().info("═══════════════════════════════════════");
        getLogger().info("MoonTalismans v" + getDescription().getVersion() + " enabled!");
        getLogger().info("Loaded " + itemManager.getItems().size() + " items");
        getLogger().info("Effects system: ACTIVE");
        getLogger().info("═══════════════════════════════════════");
    }

    @Override
    public void onDisable() {
        if (effectManager != null) {
            effectManager.shutdown();
        }
        getLogger().info("MoonTalismans disabled");
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public NamespacedKey namespaced(String key) {
        return new NamespacedKey(this, key.toLowerCase());
    }
}
