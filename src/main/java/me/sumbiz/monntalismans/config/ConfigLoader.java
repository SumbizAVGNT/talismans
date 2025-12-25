package me.sumbiz.monntalismans.config;

import me.sumbiz.monntalismans.model.*;
import me.sumbiz.monntalismans.util.DurationParser;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Logger;

public final class ConfigLoader {
    private ConfigLoader() {}

    public static Map<String, ItemDef> load(FileConfiguration cfg, Logger log) {
        Map<String, ItemDef> out = new HashMap<>();

        ConfigurationSection items = cfg.getConfigurationSection("items");
        if (items == null) return out;

        for (String id : items.getKeys(false)) {
            ConfigurationSection s = items.getConfigurationSection(id);
            if (s == null) continue;

            String typeRaw = s.getString("type", "").toLowerCase(Locale.ROOT);
            ItemType type = switch (typeRaw) {
                case "sphere" -> ItemType.SPHERE;
                case "talisman" -> ItemType.TALISMAN;
                default -> null;
            };

            if (type == null) {
                log.warning("Unknown type for item " + id + ": " + typeRaw);
                continue;
            }

            TemplateDef template = parseTemplate(s.getConfigurationSection("template"), log, id);

            String name = s.getString("name", id);
            List<String> lore = s.getStringList("lore");

            if (type == ItemType.SPHERE) {
                String b64 = s.getString("head_texture_base64", null);
                long cd = DurationParser.parseToMillis(s.getString("cooldown", "30s"));
                long gcd = DurationParser.parseToMillis(s.getString("global_cooldown", "1s"));
                int charges = Math.max(1, s.getInt("charges", 1));

                SphereDef def = new SphereDef(id, template, name, lore, b64, cd, gcd, charges);
                out.put(id, ItemDef.sphere(def));
            } else {
                TalismanDef def = new TalismanDef(id, template, name, lore);
                out.put(id, ItemDef.talisman(def));
            }
        }

        return out;
    }

    private static TemplateDef parseTemplate(ConfigurationSection s, Logger log, String id) {
        String fromNexo = null;
        Material mat = Material.PAPER;

        if (s != null) {
            fromNexo = s.getString("from_nexo", null);

            String matRaw = s.getString("material", "PAPER");
            try {
                mat = Material.valueOf(matRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                log.warning("Invalid material for " + id + ": " + matRaw + " (fallback PAPER)");
                mat = Material.PAPER;
            }
        }

        return new TemplateDef(fromNexo, mat);
    }
}
