package me.sumbiz.moontalismans.mechanics;

import me.sumbiz.moontalismans.MoonTalismansPlugin;
import me.sumbiz.moontalismans.TalismanItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Новый движок механик - работает напрямую с конфигурацией механик в предметах.
 * Убирает хардкод названий и позволяет гибко настраивать любые механики.
 */
public class MechanicEngine {
    private final MoonTalismansPlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public MechanicEngine(MoonTalismansPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Применяет пассивные механики предмета к игроку.
     */
    public void applyPassiveMechanics(Player player, TalismanItem item) {
        for (TalismanMechanic mechanic : item.getMechanics()) {
            if (!mechanic.isEnabled() || !mechanic.getType().isPassive()) {
                continue;
            }

            switch (mechanic.getType()) {
                case PASSIVE_REGEN -> applyPassiveRegen(player, mechanic);
                case LOW_HEALTH_REGEN -> applyLowHealthRegen(player, mechanic);
                case WATER_BREATHING -> applyWaterBreathing(player, mechanic);
                case FIRE_RESISTANCE -> applyFireResistance(player, mechanic);
                case ABSORPTION -> applyAbsorption(player, mechanic);
                case SATURATION -> applySaturation(player, mechanic);
                case LOW_HEALTH_STRENGTH -> applyLowHealthStrength(player, mechanic);
                case ENHANCED_JUMP -> applyEnhancedJump(player, mechanic);
                case INVISIBILITY_ON_SNEAK -> applyInvisibilityOnSneak(player, mechanic);
                case POISON_AURA -> applyPoisonAura(player, mechanic);
                case EXPERIENCE_GAIN -> applyExperienceGain(player, mechanic);
                case ITEM_MAGNET -> applyItemMagnet(player, mechanic);
                default -> {}
            }
        }
    }

    /**
     * Обрабатывает механики при атаке игрока.
     */
    public void handleAttackMechanics(Player attacker, LivingEntity target, EntityDamageByEntityEvent event, TalismanItem item) {
        for (TalismanMechanic mechanic : item.getMechanics()) {
            if (!mechanic.isEnabled() || !mechanic.getType().triggersOnAttack()) {
                continue;
            }

            switch (mechanic.getType()) {
                case POISON_ON_HIT -> handlePoisonOnHit(target, mechanic);
                case SLOWNESS_ON_HIT -> handleSlownessOnHit(target, mechanic);
                case FIRE_ON_HIT -> handleFireOnHit(target, mechanic);
                case FREEZE_ON_HIT -> handleFreezeOnHit(target, mechanic);
                case STUN_ON_HIT -> handleStunOnHit(target, mechanic);
                case LIFESTEAL -> handleLifesteal(attacker, event, mechanic);
                case CRITICAL_DAMAGE_BOOST -> handleCriticalBoost(attacker, event, mechanic);
                case RANDOM_DEBUFF_ON_HIT -> handleRandomDebuff(target, mechanic);
                case AOE_WEAKNESS -> handleAoeWeakness(target, mechanic);
                case AOE_LIFESTEAL -> handleAoeLifesteal(attacker, target, event, mechanic);
                case LIGHTNING_STRIKE -> handleLightningStrike(target, mechanic);
                case CHAIN_LIGHTNING -> handleChainLightning(target, mechanic);
                case ARMOR_PENETRATION -> handleArmorPenetration(event, mechanic);
                case ARMOR_SHRED -> handleArmorShred(target, mechanic);
                case STEAL_EFFECTS -> handleStealEffects(attacker, target, mechanic);
                default -> {}
            }
        }
    }

    /**
     * Обрабатывает механики при получении урона.
     */
    public void handleDamageMechanics(Player player, EntityDamageEvent event, TalismanItem item) {
        for (TalismanMechanic mechanic : item.getMechanics()) {
            if (!mechanic.isEnabled() || !mechanic.getType().triggersOnDamage()) {
                continue;
            }

            switch (mechanic.getType()) {
                case SPEED_ON_DAMAGE -> handleSpeedOnDamage(player, mechanic);
                case DAMAGE_REDUCTION -> handleDamageReduction(event, mechanic);
                case DAMAGE_REFLECT -> handleDamageReflect(player, event, mechanic);
                case RESISTANCE_ON_HIT -> handleResistanceOnHit(player, mechanic);
                case THORNS_DAMAGE -> handleThornsDamage(player, event, mechanic);
                case DODGE_CHANCE -> handleDodgeChance(event, mechanic);
                case EMERGENCY_TELEPORT -> handleEmergencyTeleport(player, mechanic);
                case SHIELD_ON_BLOCK -> handleShieldOnBlock(player, mechanic);
                case DAMAGE_REDIRECT -> handleDamageRedirect(player, event, mechanic);
                case LAST_STAND -> handleLastStand(player, event, mechanic);
                default -> {}
            }
        }
    }

    /**
     * Обрабатывает механики при смерти.
     */
    public boolean handleDeathMechanics(Player player, EntityDamageEvent event, TalismanItem item) {
        for (TalismanMechanic mechanic : item.getMechanics()) {
            if (!mechanic.isEnabled() || !mechanic.getType().triggersOnDeath()) {
                continue;
            }

            switch (mechanic.getType()) {
                case REVIVE_ON_DEATH -> {
                    if (handleReviveOnDeath(player, mechanic)) {
                        return true; // Cancel death
                    }
                }
                case EXPLOSION_ON_DEATH -> handleExplosionOnDeath(player, mechanic);
                default -> {}
            }
        }
        return false;
    }

    // ========== РЕАЛИЗАЦИЯ МЕХАНИК ==========

    private void applyPassiveRegen(Player player, TalismanMechanic mechanic) {
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
        }
    }

    private void applyLowHealthRegen(Player player, TalismanMechanic mechanic) {
        double threshold = mechanic.getDouble("health_threshold", 0.3);
        if (player.getHealth() < player.getMaxHealth() * threshold) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
            }
        }
    }

    private void applyWaterBreathing(Player player, TalismanMechanic mechanic) {
        if (player.isInWater()) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
            }
            // Dolphins Grace
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 100, 0, true, false, true));
        }
    }

    private void applyFireResistance(Player player, TalismanMechanic mechanic) {
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
        }
    }

    private void applyAbsorption(Player player, TalismanMechanic mechanic) {
        if (!player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
            }
        }
    }

    private void applySaturation(Player player, TalismanMechanic mechanic) {
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
        }
    }

    private void applyLowHealthStrength(Player player, TalismanMechanic mechanic) {
        double threshold = mechanic.getDouble("health_threshold", 0.5);
        if (player.getHealth() < player.getMaxHealth() * threshold) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
            }
        }
    }

    private void applyEnhancedJump(Player player, TalismanMechanic mechanic) {
        int amplifier = mechanic.getInt("amplifier", 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, amplifier, true, false, true));
    }

    private void applyInvisibilityOnSneak(Player player, TalismanMechanic mechanic) {
        if (player.isSneaking()) {
            int duration = mechanic.getInt("duration", 100);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, true, false, true));
        }
    }

    private void applyPoisonAura(Player player, TalismanMechanic mechanic) {
        double radius = mechanic.getDouble("radius", 3.0);
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity target && entity != player) {
                    target.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, true, true));
                }
            }
        }
    }

    private void applyExperienceGain(Player player, TalismanMechanic mechanic) {
        int xp = mechanic.getInt("xp_per_tick", 1);
        if (Math.random() < 0.01) { // 1% chance per tick
            player.giveExp(xp);
        }
    }

    private void applyItemMagnet(Player player, TalismanMechanic mechanic) {
        double radius = mechanic.getDouble("radius", 5.0);
        player.getNearbyEntities(radius, radius, radius).stream()
            .filter(e -> e instanceof org.bukkit.entity.Item)
            .forEach(e -> {
                Location playerLoc = player.getLocation();
                Location itemLoc = e.getLocation();
                org.bukkit.util.Vector direction = playerLoc.toVector().subtract(itemLoc.toVector()).normalize();
                e.setVelocity(direction.multiply(0.3));
            });
    }

    // Механики при атаке

    private void handlePoisonOnHit(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.30);
        if (Math.random() < chance) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                target.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, true, true));
                spawnHitParticles(target.getLocation(), Particle.WITCH);
            }
        }
    }

    private void handleSlownessOnHit(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.25);
        if (Math.random() < chance) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                target.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, true, true));
                spawnHitParticles(target.getLocation(), Particle.SNOWFLAKE);
            }
        }
    }

    private void handleFireOnHit(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.30);
        if (Math.random() < chance) {
            int duration = mechanic.getInt("duration", 40);
            target.setFireTicks(duration);
            spawnHitParticles(target.getLocation(), Particle.FLAME);
        }
    }

    private void handleFreezeOnHit(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.20);
        if (Math.random() < chance) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, true, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1, true, true, true));
            spawnHitParticles(target.getLocation(), Particle.SNOWFLAKE);
        }
    }

    private void handleStunOnHit(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.20);
        if (Math.random() < chance) {
            List<TalismanMechanic.PotionEffectConfig> effects = mechanic.getPotionEffectList("effects");
            for (TalismanMechanic.PotionEffectConfig effect : effects) {
                target.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, true, true));
            }
            spawnHitParticles(target.getLocation(), Particle.SONIC_BOOM);
        }
    }

    private void handleLifesteal(Player attacker, EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        double percent = mechanic.getDouble("percent", 0.15);
        double heal = event.getDamage() * percent;
        attacker.setHealth(Math.min(attacker.getHealth() + heal, attacker.getMaxHealth()));
        spawnHitParticles(attacker.getLocation(), Particle.HEART);
    }

    private void handleCriticalBoost(Player attacker, EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        if (attacker.getFallDistance() > 0 && !attacker.isOnGround()) {
            double multiplier = mechanic.getDouble("multiplier", 1.20);
            event.setDamage(event.getDamage() * multiplier);
            spawnHitParticles(event.getEntity().getLocation(), Particle.ENCHANTED_HIT);
        }
    }

    private void handleRandomDebuff(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.35);
        if (Math.random() < chance) {
            PotionEffectType[] effects = {
                PotionEffectType.POISON, PotionEffectType.WITHER,
                PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS
            };
            PotionEffectType random = effects[(int) (Math.random() * effects.length)];
            int duration = mechanic.getInt("duration", 60);
            int amplifier = mechanic.getInt("amplifier", 0);
            target.addPotionEffect(new PotionEffect(random, duration, amplifier, true, true, true));
            spawnHitParticles(target.getLocation(), Particle.WITCH);
        }
    }

    private void handleAoeWeakness(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.25);
        if (Math.random() < chance) {
            double radius = mechanic.getDouble("radius", 3.0);
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity living) {
                        living.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, true, true));
                    }
                }
                spawnHitParticles(target.getLocation(), Particle.SCULK_CHARGE_POP);
            }
        }
    }

    private void handleAoeLifesteal(Player attacker, LivingEntity target, EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        double radius = mechanic.getDouble("radius", 4.0);
        double percent = mechanic.getDouble("percent", 0.10);
        double totalHeal = 0;

        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity living && entity != attacker) {
                double damage = Math.min(living.getHealth(), event.getDamage() * 0.5);
                living.damage(damage, attacker);
                totalHeal += damage * percent;
            }
        }

        if (totalHeal > 0) {
            attacker.setHealth(Math.min(attacker.getHealth() + totalHeal, attacker.getMaxHealth()));
            spawnHitParticles(attacker.getLocation(), Particle.HEART);
        }
    }

    private void handleLightningStrike(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.10);
        if (Math.random() < chance) {
            target.getWorld().strikeLightningEffect(target.getLocation());
            double damage = mechanic.getDouble("damage", 5.0);
            target.damage(damage);
        }
    }

    private void handleChainLightning(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.15);
        if (Math.random() < chance) {
            double radius = mechanic.getDouble("radius", 5.0);
            double damage = mechanic.getDouble("damage", 3.0);
            int maxTargets = mechanic.getInt("max_targets", 3);

            List<LivingEntity> targets = new ArrayList<>();
            for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity living && targets.size() < maxTargets) {
                    targets.add(living);
                    living.damage(damage);
                    living.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, living.getLocation(), 20);
                }
            }
        }
    }

    private void handleArmorPenetration(EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.20);
        if (Math.random() < chance) {
            double multiplier = mechanic.getDouble("multiplier", 1.30);
            event.setDamage(event.getDamage() * multiplier);
        }
    }

    private void handleArmorShred(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.25);
        if (Math.random() < chance) {
            int duration = mechanic.getInt("duration", 100);
            int amplifier = mechanic.getInt("amplifier", 1);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, amplifier, true, true, true));
        }
    }

    private void handleStealEffects(Player attacker, LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.15);
        if (Math.random() < chance) {
            Collection<PotionEffect> effects = target.getActivePotionEffects();
            for (PotionEffect effect : effects) {
                if (effect.getType().getCategory() == PotionEffectType.Category.BENEFICIAL) {
                    attacker.addPotionEffect(effect);
                    target.removePotionEffect(effect.getType());
                }
            }
        }
    }

    // Механики при получении урона

    private void handleSpeedOnDamage(Player player, TalismanMechanic mechanic) {
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
        }
    }

    private void handleDamageReduction(EntityDamageEvent event, TalismanMechanic mechanic) {
        double multiplier = mechanic.getDouble("multiplier", 0.90);
        event.setDamage(event.getDamage() * multiplier);
    }

    private void handleDamageReflect(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        if (event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof LivingEntity attacker) {
            double percent = mechanic.getDouble("percent", 0.15);
            double reflectDamage = event.getDamage() * percent;
            attacker.damage(reflectDamage, player);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private void handleResistanceOnHit(Player player, TalismanMechanic mechanic) {
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
        }
    }

    private void handleThornsDamage(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        if (event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof LivingEntity attacker) {
            double damage = mechanic.getDouble("damage", 2.0);
            attacker.damage(damage, player);
            spawnHitParticles(attacker.getLocation(), Particle.CRIT);
        }
    }

    private void handleDodgeChance(EntityDamageEvent event, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.15);
        if (Math.random() < chance) {
            event.setCancelled(true);
        }
    }

    private void handleEmergencyTeleport(Player player, TalismanMechanic mechanic) {
        double healthThreshold = mechanic.getDouble("health_threshold", 0.20);
        if (player.getHealth() < player.getMaxHealth() * healthThreshold) {
            String cooldownKey = "emergency_teleport";
            if (isOnCooldown(player, cooldownKey)) {
                return;
            }

            Location safeLocation = findSafeLocation(player);
            if (safeLocation != null) {
                player.teleport(safeLocation);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 50);
                long cooldown = mechanic.getLong("cooldown", 60000);
                setCooldown(player, cooldownKey, cooldown);
            }
        }
    }

    private void handleShieldOnBlock(Player player, TalismanMechanic mechanic) {
        if (player.isBlocking()) {
            int absorption = mechanic.getInt("absorption_hearts", 2);
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, absorption - 1, true, false, true));
        }
    }

    private void handleDamageRedirect(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        if (event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof LivingEntity attacker) {
            double percent = mechanic.getDouble("percent", 0.50);
            double redirected = event.getDamage() * percent;
            event.setDamage(event.getDamage() - redirected);
            attacker.damage(redirected, player);
        }
    }

    private void handleLastStand(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        double healthThreshold = mechanic.getDouble("health_threshold", 0.10);
        if (player.getHealth() - event.getFinalDamage() < player.getMaxHealth() * healthThreshold) {
            String cooldownKey = "last_stand";
            if (isOnCooldown(player, cooldownKey)) {
                return;
            }

            int duration = mechanic.getInt("duration", 60);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 4, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 2, true, false, true));
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 30);

            long cooldown = mechanic.getLong("cooldown", 120000);
            setCooldown(player, cooldownKey, cooldown);
        }
    }

    // Механики при смерти

    private boolean handleReviveOnDeath(Player player, TalismanMechanic mechanic) {
        String cooldownKey = "revive";
        if (isOnCooldown(player, cooldownKey)) {
            return false;
        }

        double healthMultiplier = mechanic.getDouble("health_multiplier", 0.30);
        player.setHealth(player.getMaxHealth() * healthMultiplier);

        List<TalismanMechanic.PotionEffectConfig> effects = mechanic.getPotionEffectList("effects");
        for (TalismanMechanic.PotionEffectConfig effect : effects) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), true, false, true));
        }

        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.LAVA, loc, 20, 0.5, 0.5, 0.5, 0);
        player.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);

        long cooldown = mechanic.getLong("cooldown", 300000);
        setCooldown(player, cooldownKey, cooldown);

        player.sendMessage(Component.text("⚔ Механика возрождения сработала!").color(NamedTextColor.GOLD));
        return true;
    }

    private void handleExplosionOnDeath(Player player, TalismanMechanic mechanic) {
        double power = mechanic.getDouble("power", 2.0);
        boolean fire = mechanic.getBoolean("fire", false);
        player.getWorld().createExplosion(player.getLocation(), (float) power, fire, false);
    }

    // ========== УТИЛИТЫ ==========

    private void spawnHitParticles(Location loc, Particle particle) {
        loc.getWorld().spawnParticle(particle, loc.add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
    }

    private boolean isOnCooldown(Player player, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Long expiry = playerCooldowns.get(ability);
        if (expiry == null) return false;

        return System.currentTimeMillis() < expiry;
    }

    private void setCooldown(Player player, String ability, long durationMs) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(ability, System.currentTimeMillis() + durationMs);
    }

    private Location findSafeLocation(Player player) {
        Location current = player.getLocation();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            double dx = (random.nextDouble() - 0.5) * 20;
            double dz = (random.nextDouble() - 0.5) * 20;
            Location test = current.clone().add(dx, 0, dz);
            test.setY(test.getWorld().getHighestBlockYAt(test));

            if (test.getBlock().getType().isAir()) {
                return test;
            }
        }

        return null;
    }

    public void cleanup() {
        cooldowns.clear();
    }
}
