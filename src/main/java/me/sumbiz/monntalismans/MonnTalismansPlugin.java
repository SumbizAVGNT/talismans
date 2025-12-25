package me.sumbiz.monntalismans;

import me.sumbiz.monntalismans.commands.TalismansCommand;
import me.sumbiz.monntalismans.config.ConfigLoader;
import me.sumbiz.monntalismans.gui.AdminGui;
import me.sumbiz.monntalismans.gui.CraftingRegistry;
import me.sumbiz.monntalismans.model.ItemDef;
import me.sumbiz.monntalismans.service.AnarchyMechanicService;
import me.sumbiz.monntalismans.service.ItemService;
import me.sumbiz.monntalismans.service.NexoBridge;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class MonnTalismansPlugin extends JavaPlugin {

    private Map<String, ItemDef> items;
    private ItemService itemService;
    private CraftingRegistry craftingRegistry;
    private AdminGui adminGui;
    private AnarchyMechanicService anarchy;
    private NexoBridge nexo;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.nexo = new NexoBridge(this);
        this.items = ConfigLoader.load(getConfig(), getLogger());
        this.itemService = new ItemService(this, items, nexo);
        this.craftingRegistry = new CraftingRegistry(this, itemService);
        this.adminGui = new AdminGui(itemService, craftingRegistry, items);
        this.anarchy = new AnarchyMechanicService(this, itemService, items);

        var cmd = getCommand("talismans");
        if (cmd != null) {
            var exec = new TalismansCommand(this, itemService, adminGui);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }

        getServer().getPluginManager().registerEvents(adminGui, this);
        getServer().getPluginManager().registerEvents(anarchy, this);

        getLogger().info("MonnTalismans enabled. Items loaded: " + items.size() + ". Nexo available: " + nexo.isAvailable());
    }

    public void reloadLocal() {
        this.items = ConfigLoader.load(getConfig(), getLogger());
        this.itemService.reload(items);
        this.adminGui.reload(items);
        this.anarchy.reload(items);
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
