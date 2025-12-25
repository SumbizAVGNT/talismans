package me.sumbiz.monntalismans.model;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffectType;

import me.sumbiz.monntalismans.model.anarchy.AnarchyMechanic;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Определение сферы (головы игрока) с поддержкой эффектов и механик Nexo.
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
        int charges,
        boolean activatable,
        SphereActivationMode activationMode,
        long globalCooldownMillis,
        int maxCharges,
        ThrowSpec throwSpec,
        UseSpec useSpec,
        Set<AnarchyMechanic> anarchyMechanics
) {
    public SphereDef(String id, boolean enabled, String displayName, List<String> lore,
                     boolean glint, Set<ItemFlag> itemFlags, ResourceDef resource,
                     String headTextureBase64, String componentsNbtFile,
                     Map<ActivationSlot, Map<Attribute, Double>> attributeModifiers,
                     long cooldownMillis, int charges) {
        this(id, enabled, displayName, lore, glint, itemFlags, resource, headTextureBase64, componentsNbtFile,
                attributeModifiers, cooldownMillis, charges, true, SphereActivationMode.RIGHT_CLICK, 0L,
                charges, null, UseSpec.empty(SphereUseTarget.PLAYER), Set.of());
    }

    public SphereDef(String id, boolean activatable, SphereActivationMode activationMode, long cooldownMillis,
                     long globalCooldownMillis, int charges, String headTextureBase64,
                     ThrowSpec throwSpec, UseSpec useSpec) {
        this(id, true, id, Collections.emptyList(), false, Set.of(), ResourceDef.defaultSphere(),
                headTextureBase64, null, Collections.emptyMap(), cooldownMillis, charges,
                activatable, activationMode, globalCooldownMillis, charges, throwSpec, useSpec, Set.of());
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
        public static ResourceDef defaultSphere() {
            return new ResourceDef(Material.PLAYER_HEAD, false, "minecraft:item/player_head", null);
        }
    }

    public record PotionTimedSpec(PotionEffectType type, int amplifier, long durationMillis) {}

    public record AoeSpec(int radius, int maxTargets, List<PotionTimedSpec> potions) {}

    public record ExplosionSpec(float power, boolean setFire, boolean breakBlocks) {}

    public record DomeSpec(int radius, int maxTargets, long durationMillis, int periodTicks,
                           List<PotionTimedSpec> potions) {}

    public record ThrowSpec(double speed, int lifetimeTicks) {}

    public record UseSpec(SphereUseTarget at, List<PotionTimedSpec> selfPotions, AoeSpec aoe,
                          ExplosionSpec explosion, DomeSpec dome) {
        public static UseSpec empty(SphereUseTarget target) {
            return new UseSpec(target, List.of(), null, null, null);
        }
    }

    // Совместимость со старыми названиями методов
    public SphereActivationMode activation() {
        return activationMode;
    }

    public UseSpec onUse() {
        return useSpec;
    }

    public int defaultCharges() {
        return charges;
    }
}
