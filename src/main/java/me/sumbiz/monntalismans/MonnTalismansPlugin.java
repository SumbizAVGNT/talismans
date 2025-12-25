package me.sumbiz.monntalismans;

import me.sumbiz.monntalismans.commands.TalismansCommand;
import me.sumbiz.monntalismans.config.ConfigLoader;
import me.sumbiz.monntalismans.model.ItemDef;
import me.sumbiz.monntalismans.service.ItemService;
import me.sumbiz.monntalismans.service.NexoBridge;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class MonnTalismansPlugin extends JavaPlugin {

    private Map<String, ItemDef> items;
    private ItemService itemService;
    private NexoBridge nexo;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.nexo = new NexoBridge(this);
        this.items = ConfigLoader.load(getConfig(), getLogger());
        this.itemService = new ItemService(this, items, nexo);

        var cmd = getCommand("talismans");
        if (cmd != null) {
            var exec = new TalismansCommand(this, itemService);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }

        getLogger().info("MonnTalismans enabled. Items loaded: " + items.size() + ". Nexo available: " + nexo.isAvailable());
    }

    public void reloadLocal() {
        this.items = ConfigLoader.load(getConfig(), getLogger());
        this.itemService.reload(items);
    }

    // Хуки для интеграции с Nexo
    public void registerOurMechanics() {
        if (nexo != null) {
            nexo.registerMechanics();
        }
    }

    public void onNexoItemsLoaded() {
        if (nexo != null) {
            nexo.refreshItems();
        }
    }
}
