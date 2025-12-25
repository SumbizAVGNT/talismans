package me.sumbiz.monntalismans.model;

import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.List;

public record SphereDef(
        String nexoId,
        boolean activatable,
        SphereActivationMode activation,
        long cooldownMillis,
        long globalCooldownMillis, // 0 = взять дефолт из config.yml
        int defaultCharges,
        String headTextureBase64,
        ThrowSpec throwSpec,       // null если не THROW
        UseSpec onUse
) {
    public record PotionTimedSpec(PotionEffectType type, int amplifier, long durationMillis) {}

    public record AoeSpec(int radius, int maxTargets, List<PotionTimedSpec> potions) {}

    public record ExplosionSpec(float power, boolean setFire, boolean breakBlocks) {}

    public record DomeSpec(int radius, int maxTargets, long durationMillis, int periodTicks, List<PotionTimedSpec> potions) {}

    public record ThrowSpec(double speed, int lifetimeTicks) {}

    public record UseSpec(
            SphereUseTarget at,
            List<PotionTimedSpec> selfPotions,
            AoeSpec aoe,
            ExplosionSpec explosion,
            DomeSpec dome
    ) {
        public static UseSpec empty(SphereUseTarget at) {
            return new UseSpec(at, Collections.emptyList(), null, null, null);
        }
    }
}
