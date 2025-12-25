package me.sumbiz.moontalismans.mechanics;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/**
 * Represents a single effect that can be applied by a talisman or sphere.
 */
public record TalismanEffect(
    EffectType type,
    PotionEffectType potionType,
    int amplifier,
    int duration,
    Particle particleType,
    int particleCount,
    double particleRadius,
    String triggerEvent,
    double chance
) {
    public enum EffectType {
        POTION,           // Apply potion effect
        PARTICLE,         // Spawn particles
        DAMAGE_BOOST,     // Extra damage on hit
        LIFESTEAL,        // Heal on damage dealt
        THORNS,           // Reflect damage
        FIRE_ASPECT,      // Set target on fire
        LIGHTNING,        // Strike lightning on hit
        SLOW_FALL,        // Passive slow falling
        WATER_BREATHING,  // Passive water breathing
        NIGHT_VISION,     // Passive night vision
        REGENERATION,     // Passive regeneration
        SPEED_BURST,      // Speed boost when taking damage
        INVULNERABILITY,  // Brief invulnerability on low health
        EXPLOSION,        // Explode on death
        TELEPORT          // Random teleport on hit
    }

    public static TalismanEffect fromConfig(ConfigurationSection section) {
        if (section == null) return null;

        String typeStr = section.getString("type", "POTION");
        EffectType type;
        try {
            type = EffectType.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            type = EffectType.POTION;
        }

        PotionEffectType potionType = null;
        String potionStr = section.getString("potion_type");
        if (potionStr != null) {
            potionType = PotionEffectType.getByName(potionStr.toUpperCase(Locale.ROOT));
        }

        int amplifier = section.getInt("amplifier", 0);
        int duration = section.getInt("duration", 200); // 10 seconds default

        Particle particleType = null;
        String particleStr = section.getString("particle");
        if (particleStr != null) {
            try {
                particleType = Particle.valueOf(particleStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }

        int particleCount = section.getInt("particle_count", 10);
        double particleRadius = section.getDouble("particle_radius", 0.5);

        String triggerEvent = section.getString("trigger", "passive");
        double chance = section.getDouble("chance", 1.0);

        return new TalismanEffect(type, potionType, amplifier, duration, particleType, particleCount, particleRadius, triggerEvent, chance);
    }
}
