package me.sumbiz.monntalismans.commands;

import me.sumbiz.monntalismans.MonnTalismansPlugin;
import me.sumbiz.monntalismans.service.ItemService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class TalismansCommand implements CommandExecutor, TabCompleter {

    private final MonnTalismansPlugin plugin;
    private final ItemService items;

    public TalismansCommand(MonnTalismansPlugin plugin, ItemService items) {
        this.plugin = plugin;
        this.items = items;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(Component.text("/talismans give <player> <id> [amount]").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/talismans reload").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/talismans debug item").color(NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            if (!sender.hasPermission("talismans.admin")) {
                sender.sendMessage(Component.text("Нет прав.").color(NamedTextColor.RED));
                return true;
            }
            plugin.reloadConfig();
            plugin.reloadLocal();
            sender.sendMessage(Component.text("Перезагружено.").color(NamedTextColor.GREEN));
            return true;
        }

        if (sub.equals("give")) {
            if (!sender.hasPermission("talismans.admin")) {
                sender.sendMessage(Component.text("Нет прав.").color(NamedTextColor.RED));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(Component.text("Использование: /talismans give <player> <id> [amount]").color(NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Игрок не найден/оффлайн.").color(NamedTextColor.RED));
                return true;
            }

            String id = args[2];
            int amount = 1;
            if (args.length >= 4) {
                try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (Exception ignored) {}
            }

            var item = items.create(id, amount);
            if (item == null) {
                sender.sendMessage(Component.text("Неизвестный id в config.yml: " + id).color(NamedTextColor.RED));
                return true;
            }

            target.getInventory().addItem(item);
            sender.sendMessage(Component.text("Выдано: " + id + " x" + amount + " -> " + target.getName()).color(NamedTextColor.GREEN));
            return true;
        }

        if (sub.equals("debug") && args.length >= 2 && args[1].equalsIgnoreCase("item")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Component.text("Только для игрока.").color(NamedTextColor.RED));
                return true;
            }
            sender.sendMessage(items.debugItemInHand(p));
            return true;
        }

        sender.sendMessage(Component.text("Неизвестная команда.").color(NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("give", "reload", "debug"), args[0]);

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return items.ids().stream()
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .sorted()
                    .limit(50)
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) return filter(List.of("item"), args[1]);

        return Collections.emptyList();
    }

    private static List<String> filter(List<String> opts, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return opts.stream().filter(s -> s.startsWith(p)).collect(Collectors.toList());
    }
}
