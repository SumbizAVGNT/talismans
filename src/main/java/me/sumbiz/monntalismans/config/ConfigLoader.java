package me.sumbiz.monntalismans.config;

import me.sumbiz.monntalismans.model.*;
import me.sumbiz.monntalismans.model.anarchy.AnarchyMechanic;
import me.sumbiz.monntalismans.util.DurationParser;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;

import java.util.*;
import java.util.logging.Logger;

public final class ConfigLoader {
    private ConfigLoader() {}

    public static Map<String, ItemDef> load(FileConfiguration cfg, Logger log) {
        Map<String, ItemDef> out = new LinkedHashMap<>();

        String namespace = cfg.getString("info.namespace", "talismans");

        ConfigurationSection items = cfg.getConfigurationSection("items");
        if (items == null) {
            log.warning("No 'items' section found in config.yml");
            return out;
        }

        for (String id : items.getKeys(false)) {
            ConfigurationSection s = items.getConfigurationSection(id);
            if (s == null) continue;

            try {
                ItemDef def = parseItem(id, s, log);
                if (def != null && def.enabled()) {
                    out.put(id, def);
                    log.info("Loaded " + def.type().name().toLowerCase() + ": " + id);
                }
            } catch (Exception e) {
                log.warning("Failed to parse item '" + id + "': " + e.getMessage());
            }
        }

        return out;
    }

    private static ItemDef parseItem(String id, ConfigurationSection s, Logger log) {
        boolean enabled = s.getBoolean("enabled", true);
        String displayName = s.getString("display_name", id);
        List<String> lore = s.getStringList("lore");
        boolean glint = s.getBoolean("glint", false);
        Set<ItemFlag> itemFlags = parseItemFlags(s.getStringList("item_flags"));

        // Определяем тип по материалу
        ConfigurationSection resourceSec = s.getConfigurationSection("resource");
        Material material = Material.PAPER;
        boolean generate = false;
        String modelPath = null;
        String fromNexoId = null;

        if (resourceSec != null) {
            String matRaw = resourceSec.getString("material", "PAPER");
            try {
                material = Material.valueOf(matRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                log.warning("Invalid material for " + id + ": " + matRaw);
                material = Material.PAPER;
            }
            generate = resourceSec.getBoolean("generate", false);
            modelPath = resourceSec.getString("model_path", null);
            fromNexoId = resourceSec.getString("from_nexo", null);
        }

        // Парсим атрибуты
        Map<ActivationSlot, Map<Attribute, Double>> attrModifiers = parseAttributeModifiers(
                s.getConfigurationSection("attribute_modifiers"), log, id);

        // Это сфера если материал PLAYER_HEAD или есть head_texture_base64
        String headTexture = s.getString("head_texture_base64", null);
        String componentsNbt = s.getString("components_nbt_file", null);
        boolean isSphere = material == Material.PLAYER_HEAD || headTexture != null || componentsNbt != null;

        if (isSphere) {
            SphereDef.ResourceDef resDef = new SphereDef.ResourceDef(material, generate, modelPath, fromNexoId);

            long cooldown = DurationParser.parseToMillis(s.getString("cooldown", "30s"));
            int charges = Math.max(1, s.getInt("charges", 1));

            SphereDef def = new SphereDef(
                    id, enabled, displayName, lore, glint, itemFlags,
                    resDef, headTexture, componentsNbt, attrModifiers,
                    cooldown, charges, true, SphereActivationMode.RIGHT_CLICK, 0L,
                    charges, null, SphereDef.UseSpec.empty(SphereUseTarget.PLAYER),
                    parseAnarchyMechanics(s, log, id)
            );
            return ItemDef.sphere(def);
        } else {
            TalismanDef.ResourceDef resDef = new TalismanDef.ResourceDef(material, generate, modelPath, fromNexoId);

            TalismanDef def = new TalismanDef(
                    id, enabled, displayName, lore, glint, itemFlags,
                    resDef, attrModifiers, Set.of(ActivationSlot.OFFHAND),
                    TalismanStackingMode.NO_STACK, List.of(), Map.of(), Map.of(), Map.of(),
                    parseAnarchyMechanics(s, log, id)
            );
            return ItemDef.talisman(def);
        }
    }

    private static Set<AnarchyMechanic> parseAnarchyMechanics(ConfigurationSection section, Logger log, String itemId) {
        List<String> raw = section.getStringList("anarchy_mechanics");
        if (raw == null || raw.isEmpty()) return Set.of();

        Set<AnarchyMechanic> result = EnumSet.noneOf(AnarchyMechanic.class);
        for (String key : raw) {
            try {
                result.add(AnarchyMechanic.valueOf(key.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                log.warning("Unknown anarchy mechanic '" + key + "' for item " + itemId);
            }
        }
        return result;
    }

    private static Set<ItemFlag> parseItemFlags(List<String> flags) {
        Set<ItemFlag> result = EnumSet.noneOf(ItemFlag.class);
        if (flags == null) return result;

        for (String f : flags) {
            try {
                result.add(ItemFlag.valueOf(f.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    private static Map<ActivationSlot, Map<Attribute, Double>> parseAttributeModifiers(
            ConfigurationSection section, Logger log, String itemId) {

        Map<ActivationSlot, Map<Attribute, Double>> result = new EnumMap<>(ActivationSlot.class);
        if (section == null) return result;

        for (String slotKey : section.getKeys(false)) {
            ActivationSlot slot = ActivationSlot.fromString(slotKey);

            ConfigurationSection attrs = section.getConfigurationSection(slotKey);
            if (attrs == null) continue;

            Map<Attribute, Double> attrMap = new EnumMap<>(Attribute.class);

            for (String attrKey : attrs.getKeys(false)) {
                Attribute attr = parseAttribute(attrKey);
                if (attr == null) {
                    log.warning("Unknown attribute '" + attrKey + "' in item " + itemId);
                    continue;
                }

                double value = attrs.getDouble(attrKey, 0.0);
                attrMap.put(attr, value);
            }

            if (!attrMap.isEmpty()) {
                result.put(slot, attrMap);
            }
        }

        return result;
    }

    private static Attribute parseAttribute(String key) {
        if (key == null) return null;

        // Поддержка camelCase и snake_case
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "attackdamage", "attack_damage", "damage" -> Attribute.GENERIC_ATTACK_DAMAGE;
            case "attackspeed", "attack_speed" -> Attribute.GENERIC_ATTACK_SPEED;
            case "maxhealth", "max_health", "health" -> Attribute.GENERIC_MAX_HEALTH;
            case "armor" -> Attribute.GENERIC_ARMOR;
            case "armortoughness", "armor_toughness", "toughness" -> Attribute.GENERIC_ARMOR_TOUGHNESS;
            case "movementspeed", "movement_speed", "speed" -> Attribute.GENERIC_MOVEMENT_SPEED;
            case "luck" -> Attribute.GENERIC_LUCK;
            case "knockbackresistance", "knockback_resistance" -> Attribute.GENERIC_KNOCKBACK_RESISTANCE;
            case "attackknockback", "attack_knockback" -> Attribute.GENERIC_ATTACK_KNOCKBACK;
            case "flyingspeed", "flying_speed" -> Attribute.GENERIC_FLYING_SPEED;
            default -> {
                // Попробуем напрямую как Bukkit Attribute
                try {
                    yield Attribute.valueOf(key.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
