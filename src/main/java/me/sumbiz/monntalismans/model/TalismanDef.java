package me.sumbiz.monntalismans.model;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Определение талисмана с поддержкой атрибутов, эффектов и механик Nexo.
 */
public record TalismanDef(
        String id,
        boolean enabled,
        String displayName,
        List<String> lore,
        boolean glint,
        Set<ItemFlag> itemFlags,
        ResourceDef resource,
        Map<ActivationSlot, Map<Attribute, Double>> attributeModifiers,
        Set<ActivationSlot> activeIn,
        TalismanStackingMode stackingMode,
        List<PotionSpec> potions,
        Map<Attribute, Double> extraAttributes,
        Map<PotionEffectType, Integer> potionCaps,
        Map<Attribute, Double> attributeCaps
) {
    public TalismanDef(String id, boolean enabled, String displayName, List<String> lore,
                       boolean glint, Set<ItemFlag> itemFlags, ResourceDef resource,
                       Map<ActivationSlot, Map<Attribute, Double>> attributeModifiers) {
        this(id, enabled, displayName, lore, glint, itemFlags, resource, attributeModifiers,
                Set.of(ActivationSlot.OFFHAND), TalismanStackingMode.NO_STACK, List.of(),
                Map.of(), Map.of(), Map.of());
    }

    public TalismanDef(String id, Set<ActivationSlot> activeIn, TalismanStackingMode stackingMode,
                       List<PotionSpec> potions, Map<Attribute, Double> attributes,
                       Map<PotionEffectType, Integer> potionCaps, Map<Attribute, Double> attributeCaps) {
        this(id, true, id, Collections.emptyList(), false, Set.of(), ResourceDef.defaultTalisman(),
                Map.of(), activeIn, stackingMode, potions, attributes, potionCaps, attributeCaps);
    }

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

    public record PotionSpec(PotionEffectType type, int amplifier, int amplifierStep,
                             boolean ambient, boolean particles, boolean icon) {}

    // Совместимость со старой версией API
    public Map<Attribute, Double> attributes() {
        return extraAttributes;
    }

    public TalismanStackingMode stacking() {
        return stackingMode;
    }

    public Map<PotionEffectType, Integer> potionAmpCaps() {
        return potionCaps;
    }
}
