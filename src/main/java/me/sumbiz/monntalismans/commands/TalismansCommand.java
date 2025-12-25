package me.sumbiz.monntalismans.commands;

import me.sumbiz.monntalismans.MonnTalismansPlugin;
import me.sumbiz.monntalismans.service.ItemService;
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
            sender.sendMessage("§e/talismans give <player> <id> [amount]");
            sender.sendMessage("§e/talismans reload");
            sender.sendMessage("§e/talismans debug item");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            if (!sender.hasPermission("talismans.admin")) {
                sender.sendMessage("§cНет прав.");
                return true;
            }
            plugin.reloadConfig();
            plugin.reloadLocal();
            sender.sendMessage("§aПерезагружено.");
            return true;
        }

        if (sub.equals("give")) {
            if (!sender.hasPermission("talismans.admin")) {
                sender.sendMessage("§cНет прав.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cИспользование: /talismans give <player> <id> [amount]");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден/оффлайн.");
                return true;
            }

            String id = args[2];
            int amount = 1;
            if (args.length >= 4) {
                try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (Exception ignored) {}
            }

            var item = items.create(id, amount);
            if (item == null) {
                sender.sendMessage("§cНеизвестный id в config.yml: " + id);
                return true;
            }

            target.getInventory().addItem(item);
            sender.sendMessage("§aВыдано: " + id + " x" + amount + " -> " + target.getName());
            return true;
        }

        if (sub.equals("debug") && args.length >= 2 && args[1].equalsIgnoreCase("item")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cТолько для игрока.");
                return true;
            }
            sender.sendMessage(items.debugItemInHand(p));
            return true;
        }

        sender.sendMessage("§cНеизвестная команда.");
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
