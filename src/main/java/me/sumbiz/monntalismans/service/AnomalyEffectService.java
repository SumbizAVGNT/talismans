package me.sumbiz.monntalismans.service;

import com.nexomc.nexo.api.NexoItems;
import me.sumbiz.monntalismans.util.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

public final class AnomalyEffectService {

    private static final long DEFAULT_INTERVAL_MS = 30_000L;
    private static final long DEFAULT_DURATION_MS = 30_000L;

    private final Plugin plugin;
    private final ItemService itemService;
    private final Random random = new Random();

    private int taskId = -1;
    private long intervalTicks = 20L * 30L;

    private boolean enabled;
    private String talismanId;
    private double negativeProbability;
    private int durationTicks;
    private List<EffectSpec> positiveEffects = Collections.emptyList();
    private List<EffectSpec> negativeEffects = Collections.emptyList();

    public AnomalyEffectService(Plugin plugin, ItemService itemService) {
        this.plugin = plugin;
        this.itemService = itemService;
        reload();
    }

    public void reload() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("items.anomaly.mechanics.anomaly");
        if (section == null) {
            section = plugin.getConfig().getConfigurationSection("effects.anomaly");
        }
        Logger log = plugin.getLogger();

        if (section == null) {
            enabled = false;
            cancelTask();
            return;
        }

        enabled = section.getBoolean("enabled", true);
        talismanId = section.getString("talisman_id", "anomaly");

        long intervalMs = DurationParser.parseToMillis(section.getString("interval", "30s"));
        intervalMs = Math.max(1000L, intervalMs == 0 ? DEFAULT_INTERVAL_MS : intervalMs);
        intervalTicks = Math.max(20L, intervalMs / 50L);

        long durationMs = DurationParser.parseToMillis(section.getString("duration", "30s"));
        durationMs = Math.max(1000L, durationMs == 0 ? DEFAULT_DURATION_MS : durationMs);
        durationTicks = (int) Math.max(20L, durationMs / 50L);

        double positiveChance = clamp(section.getDouble("positive_chance", 0.95));
        double negativeChance = clamp(section.getDouble("negative_chance", 0.05));
        double total = positiveChance + negativeChance;
        if (total <= 0.0) {
            negativeProbability = 0.05;
        } else {
            negativeProbability = negativeChance / total;
        }

        positiveEffects = parseEffectSpecs(section.getList("positive_whitelist"), log, "positive_whitelist");
        negativeEffects = parseEffectSpecs(section.getList("negative_whitelist"), log, "negative_whitelist");

        if (!enabled) {
            cancelTask();
            return;
        }

        scheduleTask();
    }

    public void shutdown() {
        cancelTask();
    }

    private void scheduleTask() {
        cancelTask();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::applyTick, intervalTicks, intervalTicks);
    }

    private void cancelTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void applyTick() {
        if (!enabled || talismanId == null || talismanId.isBlank()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!hasAnomalyTalisman(player)) continue;

            if (hasAnyActivePotionEffect(player)) continue;

            List<EffectSpec> positiveAvailable = filterAvailable(player, positiveEffects);
            List<EffectSpec> negativeAvailable = filterAvailable(player, negativeEffects);

            EffectSpec toApply = chooseEffect(positiveAvailable, negativeAvailable);
            if (toApply == null) continue;

            player.addPotionEffect(new PotionEffect(
                    toApply.type(),
                    durationTicks,
                    Math.max(0, toApply.amplifier()),
                    toApply.ambient(),
                    toApply.particles(),
                    toApply.icon()
            ));
        }
    }

    private boolean hasAnyActivePotionEffect(Player player) {
        return !player.getActivePotionEffects().isEmpty();
    }

    private boolean hasAnomalyTalisman(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType().isAir()) return false;

        String id = itemService.getItemId(offhand);
        if (id == null) {
            try {
                id = NexoItems.idFromItem(offhand);
            } catch (Throwable ignored) {}
        }
        return talismanId.equalsIgnoreCase(id);
    }

    private EffectSpec chooseEffect(List<EffectSpec> positiveAvailable, List<EffectSpec> negativeAvailable) {
        boolean tryNegative = random.nextDouble() < negativeProbability;

        if (tryNegative) {
            EffectSpec neg = pickRandom(negativeAvailable);
            if (neg != null) return neg;
            return pickRandom(positiveAvailable);
        }

        EffectSpec pos = pickRandom(positiveAvailable);
        if (pos != null) return pos;
        return pickRandom(negativeAvailable);
    }

    private List<EffectSpec> filterAvailable(Player player, List<EffectSpec> specs) {
        if (specs == null || specs.isEmpty()) return Collections.emptyList();
        List<EffectSpec> out = new ArrayList<>();
        for (EffectSpec spec : specs) {
            if (!player.hasPotionEffect(spec.type())) {
                out.add(spec);
            }
        }
        return out;
    }

    private EffectSpec pickRandom(List<EffectSpec> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }

    private List<EffectSpec> parseEffectSpecs(List<?> raw, Logger log, String key) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        List<EffectSpec> result = new ArrayList<>();
        for (Object entry : raw) {
            if (entry instanceof String s) {
                EffectSpec spec = parseEffectSpecFromString(s, log, key);
                if (spec != null) result.add(spec);
                continue;
            }

            if (entry instanceof Map<?, ?> map) {
                String typeRaw = Objects.toString(map.get("type"), null);
                EffectSpec spec = parseEffectSpecFromMap(typeRaw, map, log, key);
                if (spec != null) result.add(spec);
                continue;
            }

            log.warning("[Anomaly] Unsupported entry in " + key + ": " + entry);
        }

        return result;
    }

    private EffectSpec parseEffectSpecFromString(String raw, Logger log, String key) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.split(":", 2);
        PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
        if (type == null) {
            log.warning("[Anomaly] Unknown potion type '" + raw + "' in " + key);
            return null;
        }
        int amp = 0;
        if (parts.length > 1) {
            try {
                amp = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {}
        }
        return new EffectSpec(type, amp, true, false, true);
    }

    private EffectSpec parseEffectSpecFromMap(String typeRaw, Map<?, ?> map, Logger log, String key) {
        if (typeRaw == null || typeRaw.isBlank()) return null;
        PotionEffectType type = PotionEffectType.getByName(typeRaw.trim().toUpperCase(Locale.ROOT));
        if (type == null) {
            log.warning("[Anomaly] Unknown potion type '" + typeRaw + "' in " + key);
            return null;
        }

        int amp = intOr(map.get("amplifier"), 0);
        boolean ambient = boolOr(map.get("ambient"), true);
        boolean particles = boolOr(map.get("particles"), false);
        boolean icon = boolOr(map.get("icon"), true);

        return new EffectSpec(type, amp, ambient, particles, icon);
    }

    private static int intOr(Object o, int def) {
        if (o == null) return def;
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean boolOr(Object o, boolean def) {
        if (o == null) return def;
        return Boolean.parseBoolean(String.valueOf(o));
    }

    private static double clamp(double val) {
        if (val < 0.0) return 0.0;
        if (val > 1.0) return 1.0;
        return val;
    }

    private record EffectSpec(PotionEffectType type, int amplifier, boolean ambient, boolean particles, boolean icon) {}
}
