package me.sumbiz.moontalismans;

import me.sumbiz.moontalismans.listeners.MenuListener;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MoonTalismansPlugin extends JavaPlugin {
    private ItemManager itemManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        itemManager = new ItemManager(this);
        itemManager.reload();

        MenuListener menuListener = new MenuListener(this);
        Bukkit.getPluginManager().registerEvents(menuListener, this);

        PluginCommand cmd = getCommand("talismans");
        if (cmd != null) {
            cmd.setExecutor(new TalismansCommand(this));
            cmd.setTabCompleter(new TalismansCommand(this));
        }
        getLogger().info("MoonTalismans enabled with " + itemManager.getItems().size() + " items");
    }

    @Override
    public void onDisable() {
        getLogger().info("MoonTalismans disabled");
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public NamespacedKey namespaced(String key) {
        return new NamespacedKey(this, key.toLowerCase());
    }
}
