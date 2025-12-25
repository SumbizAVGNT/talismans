package me.sumbiz.moontalismans;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class TalismanItem {
    private final String id;
    private final boolean enabled;
    private final String displayName;
    private final List<String> lore;
    private final Material material;
    private final boolean glint;
    private final List<ItemFlag> itemFlags;
    private final String headTexture;
    private final List<Material> shapelessRecipe;
    private final Map<EquipmentSlotGroup, Map<Attribute, Double>> attributeModifiers;

    // Keys for PDC
    private static NamespacedKey KEY_ID;
    private static NamespacedKey KEY_TYPE;

    public TalismanItem(String id, boolean enabled, String displayName, List<String> lore,
                       Material material, boolean glint, List<ItemFlag> itemFlags,
                       String headTexture, List<Material> shapelessRecipe,
                       Map<EquipmentSlotGroup, Map<Attribute, Double>> attributeModifiers) {
        this.id = id;
        this.enabled = enabled;
        this.displayName = displayName;
        this.lore = lore;
        this.material = material;
        this.glint = glint;
        this.itemFlags = itemFlags;
        this.headTexture = headTexture;
        this.shapelessRecipe = shapelessRecipe;
        this.attributeModifiers = attributeModifiers != null ? attributeModifiers : new HashMap<>();
    }

    public static void initKeys(Plugin plugin) {
        KEY_ID = new NamespacedKey(plugin, "talisman_id");
        KEY_TYPE = new NamespacedKey(plugin, "talisman_type");
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSphere() {
        return material == Material.PLAYER_HEAD || (headTexture != null && !headTexture.isEmpty());
    }

    public Optional<ShapelessRecipe> buildRecipe(MoonTalismansPlugin plugin) {
        if (shapelessRecipe.isEmpty() || !enabled) {
            return Optional.empty();
        }
        NamespacedKey key = new NamespacedKey(plugin, id.toLowerCase());
        ShapelessRecipe recipe = new ShapelessRecipe(key, createStack(plugin));
        shapelessRecipe.forEach(recipe::addIngredient);
        return Optional.of(recipe);
    }

    public ItemStack createStack(Plugin plugin) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            // Apply head texture first (before other meta changes)
            if (meta instanceof SkullMeta skullMeta && headTexture != null && !headTexture.isEmpty()) {
                PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(("moontalismans:" + id).getBytes()));
                profile.setProperty(new ProfileProperty("textures", headTexture));
                skullMeta.setPlayerProfile(profile);
            }

            // Display name
            if (displayName != null) {
                Component name = serializer().deserialize(displayName)
                        .decoration(TextDecoration.ITALIC, false);
                meta.displayName(name);
            }

            // Lore
            if (!lore.isEmpty()) {
                List<Component> loreComponents = lore.stream()
                        .map(line -> serializer().deserialize(line).decoration(TextDecoration.ITALIC, false))
                        .collect(Collectors.toList());
                meta.lore(loreComponents);
            }

            // Enchantment glint
            meta.setEnchantmentGlintOverride(glint);

            // Item flags
            itemFlags.forEach(meta::addItemFlags);

            // Apply attribute modifiers
            applyAttributes(meta, plugin);

            // Store item ID and type in PDC
            if (KEY_ID != null && KEY_TYPE != null) {
                meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, id);
                meta.getPersistentDataContainer().set(KEY_TYPE, PersistentDataType.STRING, isSphere() ? "SPHERE" : "TALISMAN");
            }

            stack.setItemMeta(meta);
        }
        return stack;
    }

    // Backward compatibility
    public ItemStack createStack() {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (meta instanceof SkullMeta skullMeta && headTexture != null && !headTexture.isEmpty()) {
                PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(("moontalismans:" + id).getBytes()));
                profile.setProperty(new ProfileProperty("textures", headTexture));
                skullMeta.setPlayerProfile(profile);
            }

            if (displayName != null) {
                Component name = serializer().deserialize(displayName)
                        .decoration(TextDecoration.ITALIC, false);
                meta.displayName(name);
            }
            if (!lore.isEmpty()) {
                List<Component> loreComponents = lore.stream()
                        .map(line -> serializer().deserialize(line).decoration(TextDecoration.ITALIC, false))
                        .collect(Collectors.toList());
                meta.lore(loreComponents);
            }
            meta.setEnchantmentGlintOverride(glint);
            itemFlags.forEach(meta::addItemFlags);

            // Apply attributes without plugin context
            applyAttributesSimple(meta);

            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void applyAttributes(ItemMeta meta, Plugin plugin) {
        if (attributeModifiers.isEmpty()) return;

        for (Map.Entry<EquipmentSlotGroup, Map<Attribute, Double>> slotEntry : attributeModifiers.entrySet()) {
            EquipmentSlotGroup slot = slotEntry.getKey();
            Map<Attribute, Double> attrs = slotEntry.getValue();

            for (Map.Entry<Attribute, Double> attrEntry : attrs.entrySet()) {
                Attribute attr = attrEntry.getKey();
                double value = attrEntry.getValue();

                String keyStr = id + "_" + attr.name().toLowerCase() + "_" + slot.toString().toLowerCase();
                UUID uuid = UUID.nameUUIDFromBytes(keyStr.getBytes(StandardCharsets.UTF_8));

                AttributeModifier modifier = new AttributeModifier(
                    uuid,
                    keyStr,
                    value,
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot
                );

                meta.addAttributeModifier(attr, modifier);
            }
        }
    }

    private void applyAttributesSimple(ItemMeta meta) {
        if (attributeModifiers.isEmpty()) return;

        for (Map.Entry<EquipmentSlotGroup, Map<Attribute, Double>> slotEntry : attributeModifiers.entrySet()) {
            EquipmentSlotGroup slot = slotEntry.getKey();
            Map<Attribute, Double> attrs = slotEntry.getValue();

            for (Map.Entry<Attribute, Double> attrEntry : attrs.entrySet()) {
                Attribute attr = attrEntry.getKey();
                double value = attrEntry.getValue();

                UUID uuid = UUID.nameUUIDFromBytes((id + "_" + attr.name() + "_" + slot.toString()).getBytes(StandardCharsets.UTF_8));

                AttributeModifier modifier = new AttributeModifier(
                    uuid,
                    id + "_" + attr.name().toLowerCase(),
                    value,
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot
                );

                meta.addAttributeModifier(attr, modifier);
            }
        }
    }

    public static Optional<TalismanItem> fromConfig(String id, ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        boolean enabled = section.getBoolean("enabled", true);
        String displayName = section.getString("display_name");
        List<String> lore = section.getStringList("lore");
        if (lore == null) lore = Collections.emptyList();

        ConfigurationSection resource = section.getConfigurationSection("resource");
        Material material = Material.matchMaterial(resource != null ? resource.getString("material", "TOTEM_OF_UNDYING") : "TOTEM_OF_UNDYING");
        if (material == null) {
            material = Material.TOTEM_OF_UNDYING;
        }

        boolean glint = section.getBoolean("glint", false);
        List<ItemFlag> flags = new ArrayList<>();
        for (String flag : section.getStringList("item_flags")) {
            try {
                flags.add(ItemFlag.valueOf(flag));
            } catch (IllegalArgumentException ignored) {
            }
        }

        String headTexture = section.getString("head_texture_base64", "");

        List<Material> recipe = new ArrayList<>();
        ConfigurationSection recipeSection = section.getConfigurationSection("recipe");
        if (recipeSection != null) {
            for (String value : recipeSection.getStringList("shapeless")) {
                Material mat = Material.matchMaterial(value);
                if (mat != null) {
                    recipe.add(mat);
                }
            }
        }

        // Parse attribute modifiers
        Map<EquipmentSlotGroup, Map<Attribute, Double>> attrModifiers = parseAttributeModifiers(
            section.getConfigurationSection("attribute_modifiers"), id
        );

        return Optional.of(new TalismanItem(id, enabled, displayName, lore, material, glint, flags, headTexture, recipe, attrModifiers));
    }

    private static Map<EquipmentSlotGroup, Map<Attribute, Double>> parseAttributeModifiers(
            ConfigurationSection section, String itemId) {

        Map<EquipmentSlotGroup, Map<Attribute, Double>> result = new HashMap<>();
        if (section == null) return result;

        for (String slotKey : section.getKeys(false)) {
            EquipmentSlotGroup slot = parseSlot(slotKey);

            ConfigurationSection attrs = section.getConfigurationSection(slotKey);
            if (attrs == null) continue;

            Map<Attribute, Double> attrMap = new HashMap<>();

            for (String attrKey : attrs.getKeys(false)) {
                Attribute attr = parseAttribute(attrKey);
                if (attr == null) {
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

    private static EquipmentSlotGroup parseSlot(String s) {
        if (s == null) return EquipmentSlotGroup.OFFHAND;
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main_hand" -> EquipmentSlotGroup.MAINHAND;
            case "offhand", "off_hand" -> EquipmentSlotGroup.OFFHAND;
            case "hand" -> EquipmentSlotGroup.HAND;
            case "head", "helmet" -> EquipmentSlotGroup.HEAD;
            case "chest", "chestplate" -> EquipmentSlotGroup.CHEST;
            case "legs", "leggings" -> EquipmentSlotGroup.LEGS;
            case "feet", "boots" -> EquipmentSlotGroup.FEET;
            case "armor" -> EquipmentSlotGroup.ARMOR;
            case "any" -> EquipmentSlotGroup.ANY;
            default -> EquipmentSlotGroup.OFFHAND;
        };
    }

    private static Attribute parseAttribute(String key) {
        if (key == null) return null;

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
                try {
                    yield Attribute.valueOf(key.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }

    public List<Material> getShapelessRecipe() {
        return shapelessRecipe;
    }

    public Map<EquipmentSlotGroup, Map<Attribute, Double>> getAttributeModifiers() {
        return attributeModifiers;
    }

    private static LegacyComponentSerializer serializer() {
        return LegacyComponentSerializer.builder().character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    }

    @Override
    public String toString() {
        return "TalismanItem{" +
                "id='" + id + '\'' +
                ", enabled=" + enabled +
                ", material=" + material +
                ", attributes=" + attributeModifiers.size() + " slots" +
                '}';
    }
}
