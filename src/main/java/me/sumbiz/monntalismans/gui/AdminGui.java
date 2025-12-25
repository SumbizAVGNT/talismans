package me.sumbiz.monntalismans.gui;

import me.sumbiz.monntalismans.model.ItemDef;
import me.sumbiz.monntalismans.service.ItemService;
import me.sumbiz.monntalismans.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminGui implements Listener {

    private final ItemService items;
    private final CraftingRegistry crafting;
    private Map<String, ItemDef> defs;

    private final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public AdminGui(ItemService items, CraftingRegistry crafting, Map<String, ItemDef> defs) {
        this.items = items;
        this.crafting = crafting;
        this.defs = defs;
    }

    public void reload(Map<String, ItemDef> defs) {
        this.defs = defs;
    }

    public void open(Player player, int page) {
        List<ItemDef> enabled = defs.values().stream()
                .sorted(Comparator.comparing(ItemDef::id))
                .toList();
        int perPage = 45;
        int maxPage = Math.max(0, (enabled.size() - 1) / perPage);
        page = Math.max(0, Math.min(page, maxPage));
        pages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(player, 54, ChatColor.DARK_PURPLE + "Talismans Admin " + (page + 1) + "/" + (maxPage + 1));

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < enabled.size(); i++) {
            ItemDef def = enabled.get(start + i);
            ItemStack stack = items.create(def.id(), 1);
            if (stack == null) continue;
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(" ");
                lore.add(ChatColor.GOLD + "Тип: " + def.type());
                CraftingRegistry.CraftingSpec spec = crafting.specFor(def.id());
                lore.add(ChatColor.YELLOW + "Крафт: " + (spec.enabled() ? "вкл" : "выкл") + " (реликвия: " + spec.relic().name() + (spec.shapeless() ? ", shapeless" : ", shaped") + ")");
                lore.add(ChatColor.AQUA + "ЛКМ - выдать, ПКМ - переключить реликвию, Q - форма крафта");
                lore.add(ChatColor.GRAY + "Shift+ЛКМ - вкл/выкл крафт, Shift+ПКМ - открыть следующую страницу");
                List<String> colored = lore.stream().map(s -> s.replace('§', '&')).toList();
                meta.lore(TextUtil.parseList(colored));
                stack.setItemMeta(meta);
            }
            inv.setItem(i, stack);
        }

        inv.setItem(45, nav(Material.ARROW, "Предыдущая", "Клик - страница назад"));
        inv.setItem(49, nav(Material.BARRIER, "Закрыть", "Выход"));
        inv.setItem(53, nav(Material.ARROW, "Следующая", "Клик - страница вперёд"));
        inv.setItem(47, nav(Material.CRAFTING_TABLE, "Создать новый", "(конструктор через GUI скоро)", "Пока: смотри README > GUI"));
        inv.setItem(51, nav(Material.BOOK, "Гайды", "Shift+ПКМ по предмету => следующая страница", "Q на предмете => shapeless/shaped"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle() == null || !event.getView().getTitle().contains("Talismans Admin")) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        int page = pages.getOrDefault(player.getUniqueId(), 0);
        int perPage = 45;
        List<ItemDef> enabled = defs.values().stream().sorted(Comparator.comparing(ItemDef::id)).toList();
        int maxPage = Math.max(0, (enabled.size() - 1) / perPage);

        if (slot == 45) { open(player, Math.max(0, page - 1)); return; }
        if (slot == 53) { open(player, Math.min(maxPage, page + 1)); return; }
        if (slot == 49) { player.closeInventory(); return; }

        int idx = page * perPage + slot;
        if (idx >= enabled.size()) return;
        ItemDef def = enabled.get(idx);

        if (event.isShiftClick() && event.isLeftClick()) {
            CraftingRegistry.CraftingSpec spec = crafting.toggle(def.id(), def);
            player.sendMessage(ChatColor.YELLOW + "Крафт " + def.id() + ": " + (spec.enabled() ? "включен" : "выключен"));
            open(player, page);
            return;
        }
        if (event.isShiftClick() && event.isRightClick()) {
            open(player, Math.min(maxPage, page + 1));
            return;
        }
        if (event.isRightClick()) {
            CraftingRegistry.CraftingSpec spec = crafting.cycleRelic(def.id(), def);
            player.sendMessage(ChatColor.AQUA + "Реликвия для " + def.id() + ": " + spec.relic());
            open(player, page);
            return;
        }
        if (event.getClick().isKeyboardClick() || event.getClick() == org.bukkit.event.inventory.ClickType.DROP
                || event.getClick() == org.bukkit.event.inventory.ClickType.CONTROL_DROP) {
            CraftingRegistry.CraftingSpec spec = crafting.toggleShape(def.id(), def);
            player.sendMessage(ChatColor.GOLD + "Форма крафта: " + (spec.shapeless() ? "shapeless" : "shaped"));
            open(player, page);
            return;
        }

        ItemStack give = items.create(def.id(), event.isLeftClick() ? 1 : 4);
        if (give != null) {
            player.getInventory().addItem(give);
            player.sendMessage(ChatColor.GREEN + "Выдано " + def.id());
        }
    }

    private ItemStack nav(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + name);
            List<String> l = List.of(lore).stream().map(s -> ChatColor.GRAY + s).map(s -> s.replace('§', '&')).toList();
            meta.lore(TextUtil.parseList(l));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
