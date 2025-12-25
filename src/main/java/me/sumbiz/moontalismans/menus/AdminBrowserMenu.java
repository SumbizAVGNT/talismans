package me.sumbiz.moontalismans.menus;

import me.sumbiz.moontalismans.MoonTalismansPlugin;
import me.sumbiz.moontalismans.TalismanItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminBrowserMenu implements InventoryHolder {
    private final MoonTalismansPlugin plugin;
    private final int page;
    private final List<TalismanItem> entries;

    public AdminBrowserMenu(MoonTalismansPlugin plugin, int page) {
        this.plugin = plugin;
        this.page = Math.max(page, 0);
        this.entries = new ArrayList<>(plugin.getItemManager().getItems().values());
    }

    public Inventory build() {
        Inventory inventory = Bukkit.createInventory(this, 54, "Talismans — страница " + (page + 1));
        int startIndex = page * 45;
        for (int slot = 0; slot < 45; slot++) {
            int index = startIndex + slot;
            if (index >= entries.size()) break;
            TalismanItem item = entries.get(index);
            ItemStack display = item.createStack();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("§7ЛКМ: выдать себе");
                lore.add("§7ПКМ: настроить крафт");
                if (!item.getShapelessRecipe().isEmpty()) {
                    lore.add("§7Текущий рецепт: " + item.getShapelessRecipe().size() + " ингредиентов");
                }
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
        }
        inventory.setItem(45, navItem("Предыдущая", Material.ARROW));
        inventory.setItem(49, navItem("Создать предмет", Material.AMETHYST_SHARD));
        inventory.setItem(53, navItem("Следующая", Material.ARROW));
        return inventory;
    }

    private ItemStack navItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 45) {
            if (page > 0) {
                player.openInventory(new AdminBrowserMenu(plugin, page - 1).build());
            }
            return;
        }
        if (event.getSlot() == 53) {
            int maxPage = (int) Math.ceil(entries.size() / 45.0) - 1;
            if (page < maxPage) {
                player.openInventory(new AdminBrowserMenu(plugin, page + 1).build());
            }
            return;
        }
        if (event.getSlot() == 49) {
            player.openInventory(new RecipeEditorMenu(plugin, null).build());
            return;
        }
        int index = page * 45 + event.getSlot();
        if (index >= entries.size()) return;
        TalismanItem item = entries.get(index);
        if (event.isLeftClick()) {
            player.getInventory().addItem(item.createStack());
            player.sendMessage("§aВыдан " + item.getId());
        } else if (event.isRightClick()) {
            player.openInventory(new RecipeEditorMenu(plugin, item.getId()).build());
        }
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(null, 54);
    }
}
