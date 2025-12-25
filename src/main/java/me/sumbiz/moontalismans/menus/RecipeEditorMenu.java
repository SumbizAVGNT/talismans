package me.sumbiz.moontalismans.menus;

import me.sumbiz.moontalismans.MoonTalismansPlugin;
import me.sumbiz.moontalismans.TalismanItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.Optional;

public class RecipeEditorMenu implements InventoryHolder {
    private final MoonTalismansPlugin plugin;
    private final String editingId;
    private Inventory inventory;

    // Slots layout
    private static final int[] INGREDIENT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30}; // 3x3 grid
    private static final int RESULT_SLOT = 24; // Preview slot
    private static final int SAVE_SLOT = 40; // Save button
    private static final int BACK_SLOT = 36; // Back button
    private static final int CLEAR_SLOT = 44; // Clear button

    public RecipeEditorMenu(MoonTalismansPlugin plugin, String editingId) {
        this.plugin = plugin;
        this.editingId = editingId;
    }

    public Inventory build() {
        String title = editingId == null ? "Новый рецепт" : "Рецепт: " + editingId;
        this.inventory = Bukkit.createInventory(this, 45, Component.text(title)
                .color(NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.BOLD, true));

        // Fill with glass panes
        ItemStack filler = createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, filler);
        }

        // Clear ingredient slots
        for (int slot : INGREDIENT_SLOTS) {
            inventory.setItem(slot, null);
        }

        // Load existing recipe
        if (editingId != null) {
            plugin.getItemManager().getItem(editingId).ifPresent(item -> {
                int slotIndex = 0;
                for (Material material : item.getShapelessRecipe()) {
                    if (slotIndex >= INGREDIENT_SLOTS.length) break;
                    inventory.setItem(INGREDIENT_SLOTS[slotIndex++], new ItemStack(material));
                }
            });
        }

        // Result preview
        updateResultPreview();

        // Control buttons
        inventory.setItem(SAVE_SLOT, createSaveButton());
        inventory.setItem(BACK_SLOT, createBackButton());
        inventory.setItem(CLEAR_SLOT, createClearButton());

        // Add helper text
        inventory.setItem(13, createHelpItem());

        return inventory;
    }

    private void updateResultPreview() {
        if (editingId != null) {
            Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(editingId);
            if (itemOpt.isPresent()) {
                ItemStack preview = itemOpt.get().createStack(plugin);
                ItemMeta meta = preview.getItemMeta();
                if (meta != null) {
                    List<Component> lore = new ArrayList<>();
                    if (meta.hasLore() && meta.lore() != null) {
                        lore.addAll(meta.lore());
                    }
                    lore.add(Component.empty());
                    lore.add(Component.text("§7━━━━━━━━━━━━━━━━━━━━"));
                    lore.add(Component.text("§e⚡ Результат крафта"));
                    meta.lore(lore);
                    preview.setItemMeta(meta);
                }
                inventory.setItem(RESULT_SLOT, preview);
            }
        } else {
            inventory.setItem(RESULT_SLOT, createNewItemPreview());
        }
    }

    private ItemStack createFiller(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSaveButton() {
        ItemStack item = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("✓ Сохранить рецепт")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Нажмите для сохранения рецепта"));
            lore.add(Component.empty());
            lore.add(Component.text("§8Рецепт сохранится в config.yml"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("◀ Назад")
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Вернуться к списку"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createClearButton() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("✗ Очистить")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Убрать все ингредиенты"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHelpItem() {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("ℹ Инструкция")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Разместите ингредиенты"));
            lore.add(Component.text("§7в сетке 3x3 слева"));
            lore.add(Component.empty());
            lore.add(Component.text("§eРецепт будет бесформенным"));
            lore.add(Component.text("§7(порядок не важен)"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNewItemPreview() {
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Новый предмет")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Будет создан новый предмет"));
            lore.add(Component.text("§7в config.yml"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isIngredientSlot(int slot) {
        for (int s : INGREDIENT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    public void handleClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();

        // Allow ingredient slot interaction
        if (isIngredientSlot(slot)) {
            // Don't cancel - let player place/take items
            return;
        }

        // Cancel click for non-ingredient slots
        event.setCancelled(true);

        // Save button
        if (slot == SAVE_SLOT) {
            save(player, event.getInventory());
            return;
        }

        // Back button
        if (slot == BACK_SLOT) {
            player.openInventory(new AdminBrowserMenu(plugin, 0).build());
            return;
        }

        // Clear button
        if (slot == CLEAR_SLOT) {
            for (int s : INGREDIENT_SLOTS) {
                inventory.setItem(s, null);
            }
            player.sendMessage("§e⚠ Ингредиенты очищены");
            return;
        }
    }

    public void handleClose(Player player, Inventory inventory) {
        // Return items to player on close without saving
        for (int slot : INGREDIENT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                // Don't return items if we're switching menus
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof AdminBrowserMenu) {
                    continue;
                }
            }
        }
    }

    private void save(Player player, Inventory inventory) {
        List<String> materials = new ArrayList<>();
        for (int slot : INGREDIENT_SLOTS) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;
            materials.add(stack.getType().name());
        }

        if (materials.isEmpty()) {
            player.sendMessage("§c✗ Рецепт пуст! Добавьте хотя бы один ингредиент.");
            return;
        }

        String targetId = editingId;
        if (targetId == null) {
            targetId = "custom_" + System.currentTimeMillis();
            player.sendMessage("§e⚠ Создан новый предмет: §f" + targetId);
            player.sendMessage("§7Настройте его в config.yml для изменения имени и атрибутов.");
        }

        plugin.getItemManager().saveShapelessRecipe(targetId, materials);

        String finalId = targetId;
        plugin.getItemManager().getItem(targetId).ifPresentOrElse(
            item -> player.sendMessage("§a✓ Рецепт сохранён для §f" + item.getId() + " §7(" + materials.size() + " ингредиентов)"),
            () -> player.sendMessage("§a✓ Рецепт сохранён для §f" + finalId + " §7(" + materials.size() + " ингредиентов)")
        );

        // Clear ingredient slots before opening new menu
        for (int slot : INGREDIENT_SLOTS) {
            inventory.setItem(slot, null);
        }

        player.openInventory(new AdminBrowserMenu(plugin, 0).build());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
