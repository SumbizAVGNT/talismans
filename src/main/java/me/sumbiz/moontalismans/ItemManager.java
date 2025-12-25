package me.sumbiz.moontalismans;

import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ItemManager {
    private final MoonTalismansPlugin plugin;
    private final Map<String, TalismanItem> items = new HashMap<>();
    private final List<String> registeredKeys = new ArrayList<>();

    public ItemManager(MoonTalismansPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        items.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                TalismanItem.fromConfig(key, child).ifPresent(item -> items.put(key, item));
            }
        }
        registerRecipes();
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
            item.set("lore", new ArrayList<>());
            ConfigurationSection resource = item.createSection("resource");
            resource.set("material", "TOTEM_OF_UNDYING");
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
        for (TalismanItem item : items.values()) {
            item.buildRecipe(plugin).ifPresent(plugin.getServer()::addRecipe);
            if (item.isEnabled()) {
                registeredKeys.add(item.getId().toLowerCase());
            }
        }
    }

    private void removePreviousRecipes() {
        Server server = plugin.getServer();
        Iterator<Recipe> iterator = server.recipeIterator();
        List<String> keys = new ArrayList<>(registeredKeys);
        registeredKeys.clear();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe != null && recipe.getResult() != null && recipe.getResult().getItemMeta() != null) {
                // Recipes are registered under moontalismans namespace to avoid collisions
            }
        }
        keys.forEach(key -> server.removeRecipe(plugin.namespaced(key)));
    }
}
