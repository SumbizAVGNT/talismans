package me.sumbiz.moontalismans;

import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.logging.Logger;

public class ItemManager {
    private final MoonTalismansPlugin plugin;
    private final Map<String, TalismanItem> items = new LinkedHashMap<>();
    private final Set<NamespacedKey> registeredRecipeKeys = new HashSet<>();

    public ItemManager(MoonTalismansPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        items.clear();

        Logger log = plugin.getLogger();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("items");

        if (section == null) {
            log.warning("No 'items' section found in config.yml");
            return;
        }

        int talismansLoaded = 0;
        int spheresLoaded = 0;

        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            Optional<TalismanItem> itemOpt = TalismanItem.fromConfig(key, child);

            if (itemOpt.isPresent()) {
                TalismanItem item = itemOpt.get();
                if (item.isEnabled()) {
                    items.put(key, item);
                    if (item.isSphere()) {
                        spheresLoaded++;
                    } else {
                        talismansLoaded++;
                    }
                }
            } else {
                log.warning("Failed to load item: " + key);
            }
        }

        registerRecipes();

        log.info("Loaded " + talismansLoaded + " talismans and " + spheresLoaded + " spheres (" + items.size() + " total)");
    }

    public Map<String, TalismanItem> getItems() {
        return items;
    }

    public Optional<TalismanItem> getItem(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public void saveShapelessRecipe(String id, List<String> materials) {
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        if (itemsSection == null) {
            itemsSection = plugin.getConfig().createSection("items");
        }

        ConfigurationSection item = itemsSection.getConfigurationSection(id);
        if (item == null) {
            item = itemsSection.createSection(id);
            item.set("enabled", true);
            item.set("display_name", "&e" + id);
            item.set("lore", Collections.singletonList("&7Новый предмет"));
            item.set("glint", false);
            item.set("item_flags", Collections.singletonList("HIDE_ATTRIBUTES"));

            ConfigurationSection resource = item.createSection("resource");
            resource.set("material", "TOTEM_OF_UNDYING");
            resource.set("generate", false);
        }

        ConfigurationSection recipeSection = item.getConfigurationSection("recipe");
        if (recipeSection == null) {
            recipeSection = item.createSection("recipe");
        }
        recipeSection.set("shapeless", materials);

        plugin.saveConfig();
        reload();
    }

    private void registerRecipes() {
        removePreviousRecipes();

        Server server = plugin.getServer();
        int registered = 0;

        for (TalismanItem item : items.values()) {
            Optional<org.bukkit.inventory.ShapelessRecipe> recipeOpt = item.buildRecipe(plugin);
            if (recipeOpt.isPresent()) {
                org.bukkit.inventory.ShapelessRecipe recipe = recipeOpt.get();
                try {
                    server.addRecipe(recipe);
                    registeredRecipeKeys.add(plugin.namespaced(item.getId().toLowerCase()));
                    registered++;
                } catch (IllegalStateException e) {
                    // Recipe already exists, try to remove and re-add
                    server.removeRecipe(plugin.namespaced(item.getId().toLowerCase()));
                    try {
                        server.addRecipe(recipe);
                        registeredRecipeKeys.add(plugin.namespaced(item.getId().toLowerCase()));
                        registered++;
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Failed to register recipe for " + item.getId() + ": " + ex.getMessage());
                    }
                }
            }
        }

        if (registered > 0) {
            plugin.getLogger().info("Registered " + registered + " recipes");
        }
    }

    private void removePreviousRecipes() {
        Server server = plugin.getServer();

        // Remove all previously registered recipes
        for (NamespacedKey key : registeredRecipeKeys) {
            try {
                server.removeRecipe(key);
            } catch (Exception ignored) {
                // Recipe might not exist anymore
            }
        }

        registeredRecipeKeys.clear();
    }
}
