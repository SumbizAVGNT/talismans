package me.sumbiz.moontalismans.listeners;

import me.sumbiz.moontalismans.menus.AdminBrowserMenu;
import me.sumbiz.moontalismans.menus.RecipeEditorMenu;
import me.sumbiz.moontalismans.MoonTalismansPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {
    private final MoonTalismansPlugin plugin;

    public MenuListener(MoonTalismansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof AdminBrowserMenu menu) {
            event.setCancelled(true);
            menu.handleClick(player, event);
        } else if (holder instanceof RecipeEditorMenu menu) {
            menu.handleClick(player, event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof RecipeEditorMenu menu) {
            menu.handleClose(player, event.getInventory());
        }
    }
}
