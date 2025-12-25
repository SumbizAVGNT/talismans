package me.sumbiz.moontalismans;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    public TalismanItem(String id, boolean enabled, String displayName, List<String> lore, Material material, boolean glint, List<ItemFlag> itemFlags, String headTexture, List<Material> shapelessRecipe) {
        this.id = id;
        this.enabled = enabled;
        this.displayName = displayName;
        this.lore = lore;
        this.material = material;
        this.glint = glint;
        this.itemFlags = itemFlags;
        this.headTexture = headTexture;
        this.shapelessRecipe = shapelessRecipe;
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<ShapelessRecipe> buildRecipe(MoonTalismansPlugin plugin) {
        if (shapelessRecipe.isEmpty() || !enabled) {
            return Optional.empty();
        }
        NamespacedKey key = new NamespacedKey(plugin, id.toLowerCase());
        ShapelessRecipe recipe = new ShapelessRecipe(key, createStack());
        shapelessRecipe.forEach(recipe::addIngredient);
        return Optional.of(recipe);
    }

    public ItemStack createStack() {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                Component name = serializer().deserialize(displayName);
                meta.displayName(name);
            }
            if (!lore.isEmpty()) {
                List<Component> loreComponents = lore.stream()
                        .map(serializer()::deserialize)
                        .collect(Collectors.toList());
                meta.lore(loreComponents);
            }
            if (glint) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            }
            itemFlags.forEach(meta::addItemFlags);
            if (meta instanceof SkullMeta skullMeta && headTexture != null && !headTexture.isEmpty()) {
                PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(("moontalismans:" + id).getBytes()));
                profile.setProperty(new ProfileProperty("textures", headTexture));
                skullMeta.setPlayerProfile(profile);
            }
            stack.setItemMeta(meta);
        }
        return stack;
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
        return Optional.of(new TalismanItem(id, enabled, displayName, lore, material, glint, flags, headTexture, recipe));
    }

    public List<Material> getShapelessRecipe() {
        return shapelessRecipe;
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
                '}';
    }
}
