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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AdminBrowserMenu implements InventoryHolder {
    private final MoonTalismansPlugin plugin;
    private final int page;
    private final List<TalismanItem> entries;
    private Inventory inventory;

    private static final int ITEMS_PER_PAGE = 45;

    public AdminBrowserMenu(MoonTalismansPlugin plugin, int page) {
        this.plugin = plugin;
        this.page = Math.max(page, 0);
        // Sort items: talismans first, then spheres, alphabetically within each group
        this.entries = plugin.getItemManager().getItems().values().stream()
                .sorted(Comparator
                        .comparing(TalismanItem::isSphere)
                        .thenComparing(TalismanItem::getId))
                .collect(Collectors.toList());
    }

    public Inventory build() {
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) ITEMS_PER_PAGE));
        Component title = Component.text("Талисманы — страница " + (page + 1) + "/" + totalPages)
                .color(NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.BOLD, true);

        this.inventory = Bukkit.createInventory(this, 54, title);

        int startIndex = page * ITEMS_PER_PAGE;
        for (int slot = 0; slot < ITEMS_PER_PAGE; slot++) {
            int index = startIndex + slot;
            if (index >= entries.size()) break;
            TalismanItem item = entries.get(index);
            ItemStack display = item.createStack(plugin);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                if (meta.hasLore() && meta.lore() != null) {
                    lore.addAll(meta.lore());
                }
                lore.add(Component.empty());
                lore.add(Component.text("§7━━━━━━━━━━━━━━━━━━━━"));
                lore.add(Component.text("§e▶ ЛКМ: §fвыдать себе"));
                lore.add(Component.text("§e▶ ПКМ: §fнастроить крафт"));
                if (!item.getShapelessRecipe().isEmpty()) {
                    lore.add(Component.text("§a✓ §7Рецепт: " + item.getShapelessRecipe().size() + " ингредиентов"));
                } else {
                    lore.add(Component.text("§c✗ §7Рецепт не настроен"));
                }
                lore.add(Component.text("§7ID: §8" + item.getId()));
                if (!item.getAttributeModifiers().isEmpty()) {
                    int attrCount = item.getAttributeModifiers().values().stream()
                            .mapToInt(m -> m.size()).sum();
                    lore.add(Component.text("§b⚔ §7Атрибутов: §f" + attrCount));
                }
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
        }

        // Navigation buttons
        inventory.setItem(45, createNavItem("◀ Назад", Material.ARROW, page > 0));
        inventory.setItem(47, createInfoItem());
        inventory.setItem(49, createNavItem("✚ Новый", Material.EMERALD, true));
        inventory.setItem(51, createReloadItem());
        inventory.setItem(53, createNavItem("Вперёд ▶", Material.ARROW, page < totalPages - 1));

        // Fill empty navigation slots with glass
        for (int i = 45; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createFiller());
            }
        }

        return inventory;
    }

    private ItemStack createNavItem(String name, Material material, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? material : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name)
                    .color(enabled ? NamedTextColor.YELLOW : NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Информация")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Всего предметов: §f" + entries.size()));
            long talismans = entries.stream().filter(i -> !i.isSphere()).count();
            long spheres = entries.stream().filter(TalismanItem::isSphere).count();
            lore.add(Component.text("§7Талисманов: §e" + talismans));
            lore.add(Component.text("§7Сфер: §b" + spheres));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createReloadItem() {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("⟳ Перезагрузить")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Нажмите чтобы перезагрузить"));
            lore.add(Component.text("§7конфигурацию плагина"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();

        // Previous page
        if (slot == 45) {
            if (page > 0) {
                player.openInventory(new AdminBrowserMenu(plugin, page - 1).build());
            }
            return;
        }

        // Next page
        if (slot == 53) {
            int maxPage = (int) Math.ceil(entries.size() / (double) ITEMS_PER_PAGE) - 1;
            if (page < maxPage) {
                player.openInventory(new AdminBrowserMenu(plugin, page + 1).build());
            }
            return;
        }

        // Create new item
        if (slot == 49) {
            player.openInventory(new RecipeEditorMenu(plugin, null).build());
            return;
        }

        // Reload config
        if (slot == 51) {
            plugin.getItemManager().reload();
            player.sendMessage("§a✓ Конфигурация перезагружена! Загружено предметов: " + plugin.getItemManager().getItems().size());
            player.openInventory(new AdminBrowserMenu(plugin, 0).build());
            return;
        }

        // Item slots (0-44)
        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            int index = page * ITEMS_PER_PAGE + slot;
            if (index >= entries.size()) return;

            TalismanItem item = entries.get(index);
            if (event.isLeftClick()) {
                ItemStack stack = item.createStack(plugin);
                player.getInventory().addItem(stack);
                player.sendMessage("§a✓ Выдан: §f" + item.getId());
            } else if (event.isRightClick()) {
                player.openInventory(new RecipeEditorMenu(plugin, item.getId()).build());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
