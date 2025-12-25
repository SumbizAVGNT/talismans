package me.sumbiz.moontalismans;

import me.sumbiz.moontalismans.menus.AdminBrowserMenu;
import me.sumbiz.moontalismans.TalismanItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class TalismansCommand implements CommandExecutor, TabExecutor {
    private final MoonTalismansPlugin plugin;

    public TalismansCommand(MoonTalismansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("moontalismans.admin")) {
            sender.sendMessage(Component.text("Недостаточно прав (moontalismans.admin)").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("/talismans give <id> [amount] [player]").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/talismans list").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/talismans reload").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/talismans debug").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/talismans gui [page]").color(NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            case "debug" -> debug(sender);
            case "gui" -> gui(sender, args);
            default -> sender.sendMessage(Component.text("Неизвестная подкоманда").color(NamedTextColor.RED));
        }
        return true;
    }

    private void gui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Только игрок может открыть GUI").color(NamedTextColor.RED));
            return;
        }
        int page = 0;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException ignored) {
            }
        }
        player.openInventory(new AdminBrowserMenu(plugin, page).build());
    }

    private void debug(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Команда только для игроков").color(NamedTextColor.RED));
            return;
        }
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            sender.sendMessage(Component.text("Держите предмет в руке").color(NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Тип предмета: " + player.getInventory().getItemInMainHand().getType()).color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Метаданные: " + player.getInventory().getItemInMainHand().getItemMeta()).color(NamedTextColor.GRAY));
    }

    private void reload(CommandSender sender) {
        plugin.getItemManager().reload();
        sender.sendMessage(Component.text("Конфигурация перезагружена. Загружено " + plugin.getItemManager().getItems().size() + " предметов.").color(NamedTextColor.GREEN));
    }

    private void list(CommandSender sender) {
        Map<String, TalismanItem> items = plugin.getItemManager().getItems();
        sender.sendMessage(Component.text("Доступные предметы: " + String.join(", ", items.keySet())).color(NamedTextColor.GRAY));
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Использование: /talismans give <id> [amount] [player]").color(NamedTextColor.RED));
            return;
        }
        String id = args[1];
        Optional<TalismanItem> optional = plugin.getItemManager().getItem(id);
        if (optional.isEmpty()) {
            sender.sendMessage(Component.text("Неизвестный предмет " + id).color(NamedTextColor.RED));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
            }
        }
        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                sender.sendMessage(Component.text("Игрок не найден").color(NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("Укажите игрока").color(NamedTextColor.RED));
            return;
        }
        TalismanItem item = optional.get();
        for (int i = 0; i < amount; i++) {
            target.getInventory().addItem(item.createStack());
        }
        sender.sendMessage(Component.text("Выдано " + amount + "x " + id + " игроку " + target.getName()).color(NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("give");
            completions.add("list");
            completions.add("reload");
            completions.add("debug");
            completions.add("gui");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getItemManager().getItems().keySet());
        }
        return completions;
    }
}
