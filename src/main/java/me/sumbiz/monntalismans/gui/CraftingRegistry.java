package me.sumbiz.monntalismans.gui;

import me.sumbiz.monntalismans.model.ItemDef;
import me.sumbiz.monntalismans.service.ItemService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public final class CraftingRegistry {

    public record CraftingSpec(boolean enabled, Material relic, boolean shapeless) {}

    private final Plugin plugin;
    private final ItemService itemService;
    private final Map<String, CraftingSpec> specs = new HashMap<>();

    private final Material[] relicCycle = new Material[]{Material.NETHER_STAR, Material.TOTEM_OF_UNDYING, Material.DRAGON_BREATH, Material.HEART_OF_THE_SEA};

    public CraftingRegistry(Plugin plugin, ItemService itemService) {
        this.plugin = plugin;
        this.itemService = itemService;
    }

    public CraftingSpec specFor(String id) {
        return specs.getOrDefault(id, new CraftingSpec(false, Material.NETHER_STAR, false));
    }

    public CraftingSpec toggle(String id, ItemDef def) {
        CraftingSpec prev = specFor(id);
        CraftingSpec next = new CraftingSpec(!prev.enabled(), prev.relic(), prev.shapeless());
        specs.put(id, next);
        syncRecipe(id, def, next);
        return next;
    }

    public CraftingSpec cycleRelic(String id, ItemDef def) {
        CraftingSpec prev = specFor(id);
        int idx = 0;
        for (int i = 0; i < relicCycle.length; i++) {
            if (relicCycle[i] == prev.relic()) { idx = i; break; }
        }
        Material nextRelic = relicCycle[(idx + 1) % relicCycle.length];
        CraftingSpec next = new CraftingSpec(prev.enabled(), nextRelic, prev.shapeless());
        specs.put(id, next);
        syncRecipe(id, def, next);
        return next;
    }

    public CraftingSpec toggleShape(String id, ItemDef def) {
        CraftingSpec prev = specFor(id);
        CraftingSpec next = new CraftingSpec(prev.enabled(), prev.relic(), !prev.shapeless());
        specs.put(id, next);
        syncRecipe(id, def, next);
        return next;
    }

    public Map<String, CraftingSpec> all() {
        return Map.copyOf(specs);
    }

    private void syncRecipe(String id, ItemDef def, CraftingSpec spec) {
        NamespacedKey key = new NamespacedKey(plugin, "craft_" + id);
        Bukkit.removeRecipe(key);
        if (!spec.enabled()) return;

        ItemStack result = itemService.create(id, 1);
        if (result == null) return;

        Material relic = spec.relic();
        Material base = switch (def.type()) {
            case SPHERE -> ((ItemDef.Sphere) def).def().resource().material();
            case TALISMAN -> ((ItemDef.Talisman) def).def().resource().material();
        };

        if (spec.shapeless()) {
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            recipe.addIngredient(relic);
            recipe.addIngredient(base);
            recipe.addIngredient(Material.NETHERITE_SCRAP);
            Bukkit.addRecipe(recipe);
        } else {
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape("RNR", " B ", "RBR");
            recipe.setIngredient('R', relic);
            recipe.setIngredient('N', Material.NETHERITE_SCRAP);
            recipe.setIngredient('B', base);
            Bukkit.addRecipe(recipe);
        }
    }
}
