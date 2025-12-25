package me.sumbiz.monntalismans.model;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemFlag;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Определение талисмана с поддержкой атрибутов и визуальных настроек.
 */
public record TalismanDef(
        String id,
        boolean enabled,
        String displayName,
        List<String> lore,
        boolean glint,
        Set<ItemFlag> itemFlags,
        ResourceDef resource,
        Map<ActivationSlot, Map<Attribute, Double>> attributeModifiers
) {
    /**
     * Определение ресурса/материала предмета.
     */
    public record ResourceDef(
            Material material,
            boolean generate,
            String modelPath,
            String fromNexoId
    ) {
        public static ResourceDef defaultTalisman() {
            return new ResourceDef(Material.TOTEM_OF_UNDYING, false, "item/talisman_totem", null);
        }
    }
}
