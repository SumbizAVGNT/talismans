package me.sumbiz.moontalismans;

import me.sumbiz.moontalismans.menus.AdminBrowserMenu;
import me.sumbiz.moontalismans.TalismanItem;
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
            sender.sendMessage("§cНедостаточно прав (moontalismans.admin)");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/talismans give <id> [amount] [player]");
            sender.sendMessage("§e/talismans list");
            sender.sendMessage("§e/talismans reload");
            sender.sendMessage("§e/talismans debug");
            sender.sendMessage("§e/talismans gui [page]");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            case "debug" -> debug(sender);
            case "gui" -> gui(sender, args);
            default -> sender.sendMessage("§cНеизвестная подкоманда");
        }
        return true;
    }

    private void gui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько игрок может открыть GUI");
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
            sender.sendMessage("§cКоманда только для игроков");
            return;
        }
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            sender.sendMessage("§cДержите предмет в руке");
            return;
        }
        sender.sendMessage("§7Тип предмета: " + player.getInventory().getItemInMainHand().getType());
        sender.sendMessage("§7Метаданные: " + player.getInventory().getItemInMainHand().getItemMeta());
    }

    private void reload(CommandSender sender) {
        plugin.getItemManager().reload();
        sender.sendMessage("§aКонфигурация перезагружена. Загружено " + plugin.getItemManager().getItems().size() + " предметов.");
    }

    private void list(CommandSender sender) {
        Map<String, TalismanItem> items = plugin.getItemManager().getItems();
        sender.sendMessage("§7Доступные предметы: " + String.join(", ", items.keySet()));
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /talismans give <id> [amount] [player]");
            return;
        }
        String id = args[1];
        Optional<TalismanItem> optional = plugin.getItemManager().getItem(id);
        if (optional.isEmpty()) {
            sender.sendMessage("§cНеизвестный предмет " + id);
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
                sender.sendMessage("§cИгрок не найден");
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage("§cУкажите игрока");
            return;
        }
        TalismanItem item = optional.get();
        for (int i = 0; i < amount; i++) {
            target.getInventory().addItem(item.createStack());
        }
        sender.sendMessage("§aВыдано " + amount + "x " + id + " игроку " + target.getName());
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
