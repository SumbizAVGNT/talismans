package me.sumbiz.moontalismans.menus;

import me.sumbiz.moontalismans.MoonTalismansPlugin;
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

public class RecipeEditorMenu implements InventoryHolder {
    private final MoonTalismansPlugin plugin;
    private final String editingId;

    public RecipeEditorMenu(MoonTalismansPlugin plugin, String editingId) {
        this.plugin = plugin;
        this.editingId = editingId;
    }

    public Inventory build() {
        Inventory inventory = Bukkit.createInventory(this, 27, editingId == null ? "Новый талисман" : "Рецепт: " + editingId);
        inventory.setItem(22, button("Сохранить рецепт", Material.LIME_CONCRETE));
        if (editingId != null) {
            plugin.getItemManager().getItem(editingId).ifPresent(item -> {
                int slot = 0;
                for (Material material : item.getShapelessRecipe()) {
                    inventory.setItem(slot++, new ItemStack(material));
                    if (slot >= 9) break;
                }
            });
        }
        return inventory;
    }

    private ItemStack button(String name, Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a" + name);
            List<String> lore = new ArrayList<>();
            lore.add("§7Заполните первые 9 слотов ингредиентами");
            lore.add("§7Можно использовать любой предмет для настройки рецепта");
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void handleClick(Player player, InventoryClickEvent event) {
        if (event.getSlot() == 22) {
            save(player, event.getInventory());
            return;
        }
        if (event.getSlot() >= 9 && event.getSlot() < 27 && event.getSlot() != 22) {
            event.setCancelled(true);
        }
    }

    public void handleClose(Player player, Inventory inventory) {
        // auto save on close to avoid losing work
        save(player, inventory);
    }

    private void save(Player player, Inventory inventory) {
        List<String> materials = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) continue;
            materials.add(stack.getType().name());
        }
        String targetId = editingId;
        if (targetId == null) {
            targetId = "custom_" + System.currentTimeMillis();
            player.sendMessage("§eСоздан новый предмет " + targetId + ". Добавьте его в config.yml для дальнейшей настройки.");
        }
        plugin.getItemManager().saveShapelessRecipe(targetId, materials);
        String finalId = targetId;
        plugin.getItemManager().getItem(targetId).ifPresentOrElse(
                item -> player.sendMessage("§aРецепт сохранён для " + item.getId() + " (" + materials.size() + " ингредиентов)")
                , () -> player.sendMessage("§aРецепт сохранён для " + finalId + " (" + materials.size() + " ингредиентов)")
        );
        player.openInventory(new AdminBrowserMenu(plugin, 0).build());
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(null, 27);
    }
}
