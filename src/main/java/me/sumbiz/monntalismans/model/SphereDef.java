package me.sumbiz.monntalismans.model;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemFlag;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Определение сферы (голова игрока с текстурой) с атрибутами.
 */
public record SphereDef(
        String id,
        boolean enabled,
        String displayName,
        List<String> lore,
        boolean glint,
        Set<ItemFlag> itemFlags,
        ResourceDef resource,
        String headTextureBase64,
        String componentsNbtFile,
        Map<ActivationSlot, Map<Attribute, Double>> attributeModifiers,
        long cooldownMillis,
        int charges
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
        public static ResourceDef defaultSphere() {
            return new ResourceDef(Material.PLAYER_HEAD, false, "minecraft:item/player_head", null);
        }
    }
}
