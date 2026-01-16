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
                case BLOOD_MOON -> applyBloodMoon(player, mechanic);
                case SOLAR_FLARE -> applySolarFlare(player, mechanic);
                case ELEMENTAL_IMMUNITY -> applyElementalImmunity(player, mechanic);
                case SPIRIT_WALK -> applySpiritWalk(player, mechanic);
                case POISON_IMMUNITY -> applyPoisonImmunity(player, mechanic);
                case WITHER_IMMUNITY -> applyWitherImmunity(player, mechanic);
                case KNOCKBACK_IMMUNITY -> applyKnockbackImmunity(player, mechanic);
                case FALL_DAMAGE_IMMUNITY -> applyFallDamageImmunity(player, mechanic);
                case EXPLOSION_IMMUNITY -> applyExplosionImmunity(player, mechanic);
                case MAGIC_BARRIER -> applyMagicBarrier(player, mechanic);
                case ANGEL_WINGS -> applyAngelWings(player, mechanic);
                case DARK_PACT -> applyDarkPact(player, mechanic);
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
                case PHASE_SHIFT -> handlePhaseShift(attacker, mechanic);
                case VOID_STRIKE -> handleVoidStrike(event, mechanic);
                case CHAOS_DAMAGE -> handleChaosDamage(target, mechanic);
                case CURSE_ON_HIT -> handleCurseOnHit(target, mechanic);
                case HOLY_SMITE -> handleHolySmite(target, event, mechanic);
                case COMBO_DAMAGE -> handleComboDamage(attacker, event, mechanic);
                case WHIRLWIND -> handleWhirlwind(attacker, target, event, mechanic);
                case CLEAVE -> handleCleave(attacker, target, event, mechanic);
                case BLEED_ON_HIT -> handleBleedOnHit(target, mechanic);
                case LIFE_TAP -> handleLifeTap(attacker, event, mechanic);
                case EXECUTE -> handleExecute(target, event, mechanic);
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
                case DARKNESS_ON_HIT -> handleDarknessOnHit(player, event, mechanic);
                case DODGE_CHANCE -> handleDodgeChance(event, mechanic);
                case EMERGENCY_TELEPORT -> handleEmergencyTeleport(player, mechanic);
                case SHIELD_ON_BLOCK -> handleShieldOnBlock(player, mechanic);
                case DAMAGE_REDIRECT -> handleDamageRedirect(player, event, mechanic);
                case LAST_STAND -> handleLastStand(player, event, mechanic);
                case MANA_SHIELD -> handleManaShield(player, event, mechanic);
                case REFLECT_PROJECTILES -> handleReflectProjectiles(player, event, mechanic);
                case TIME_SLOW -> handleTimeSlow(player, event, mechanic);
                case RUNE_SHIELD -> handleRuneShield(player, event, mechanic);
                case ELEMENTAL_ABSORPTION -> handleElementalAbsorption(player, event, mechanic);
                case SPECTRAL_FORM -> handleSpectralForm(player, event, mechanic);
                case FROST_NOVA -> handleFrostNova(player, mechanic);
                case SHOCK_WAVE -> handleShockWave(player, mechanic);
                case COUNTER_ATTACK -> handleCounterAttack(player, event, mechanic);
                case PROJECTILE_DEFLECT -> handleProjectileDeflect(event, mechanic);
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
                case DIVINE_INTERVENTION -> {
                    if (handleDivineIntervention(player, mechanic)) {
                        return true; // Cancel death
                    }
                }
                default -> {}
            }
        }
        return false;
    }

    // ========== РЕАЛИЗАЦИЯ МЕХАНИК ==========

    private void applyPassiveRegen(Player player, TalismanMechanic mechanic) {
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
        }
    }

    private void applyLowHealthRegen(Player player, TalismanMechanic mechanic) {
        double threshold = mechanic.getDouble("health_threshold", 0.3);
        if (player.getHealth() < player.getMaxHealth() * threshold) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
            }
        }
    }

    private void applyWaterBreathing(Player player, TalismanMechanic mechanic) {
        if (player.isInWater()) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
            }
            // Dolphins Grace
            player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.DOLPHINS_GRACE, 100, 0));
        }
    }

    private void applyFireResistance(Player player, TalismanMechanic mechanic) {
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
        }
    }

    private void applyAbsorption(Player player, TalismanMechanic mechanic) {
        if (!player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
            }
        }
    }

    private void applySaturation(Player player, TalismanMechanic mechanic) {
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
        }
    }

    private void applyLowHealthStrength(Player player, TalismanMechanic mechanic) {
        double threshold = mechanic.getDouble("health_threshold", 0.5);
        if (player.getHealth() < player.getMaxHealth() * threshold) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
            }
        }
    }

    private void applyEnhancedJump(Player player, TalismanMechanic mechanic) {
        int amplifier = mechanic.getInt("amplifier", 1);
        player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.JUMP_BOOST, 100, amplifier));
    }

    private void applyInvisibilityOnSneak(Player player, TalismanMechanic mechanic) {
        if (player.isSneaking()) {
            int duration = mechanic.getInt("duration", 100);
            player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.INVISIBILITY, duration, 0));
        }
    }

    private void applyPoisonAura(Player player, TalismanMechanic mechanic) {
        double radius = mechanic.getDouble("radius", 3.0);
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        if (effect != null) {
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity target && entity != player) {
                    target.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
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
                target.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
                spawnHitParticles(target.getLocation(), Particle.WITCH);
            }
        }
    }

    private void handleSlownessOnHit(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.25);
        if (Math.random() < chance) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                target.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
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
                target.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
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
                        living.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
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
                // Steal beneficial effects (positive potion effects)
                PotionEffectType type = effect.getType();
                if (type.equals(PotionEffectType.SPEED) || type.equals(PotionEffectType.STRENGTH) ||
                    type.equals(PotionEffectType.JUMP_BOOST) || type.equals(PotionEffectType.REGENERATION) ||
                    type.equals(PotionEffectType.RESISTANCE) || type.equals(PotionEffectType.FIRE_RESISTANCE) ||
                    type.equals(PotionEffectType.WATER_BREATHING) || type.equals(PotionEffectType.INVISIBILITY) ||
                    type.equals(PotionEffectType.NIGHT_VISION) || type.equals(PotionEffectType.HEALTH_BOOST) ||
                    type.equals(PotionEffectType.ABSORPTION) || type.equals(PotionEffectType.SATURATION)) {
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
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
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
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
        }
    }

    private void handleThornsDamage(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        if (event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof LivingEntity attacker) {
            double damage = mechanic.getDouble("damage", 2.0);
            attacker.damage(damage, player);
            spawnHitParticles(attacker.getLocation(), Particle.CRIT);
        }
    }

    private void handleDarknessOnHit(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        if (event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof Player attacker) {
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                attacker.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
                spawnHitParticles(attacker.getLocation(), Particle.SONIC_BOOM);
            }
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
            player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.ABSORPTION, 100, absorption - 1));
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
            player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.RESISTANCE, duration, 4));
            player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.REGENERATION, duration, 2));
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
            player.addPotionEffect(new PotionEffect(effect.type(), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()));
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

    private boolean handleDivineIntervention(Player player, TalismanMechanic mechanic) {
        String cooldownKey = "divine_intervention";
        if (isOnCooldown(player, cooldownKey)) {
            return false;
        }

        double healthMultiplier = mechanic.getDouble("health_multiplier", 0.50);
        player.setHealth(player.getMaxHealth() * healthMultiplier);

        player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.RESISTANCE, 100, 4));
        player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.REGENERATION, 200, 3));

        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 100, 1, 2, 1, 0.1);
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 2.0f);

        long cooldown = mechanic.getLong("cooldown", 600000);
        setCooldown(player, cooldownKey, cooldown);

        player.sendMessage(Component.text("✝ Божественное вмешательство спасло вас!").color(NamedTextColor.LIGHT_PURPLE));
        return true;
    }

    // ========== НОВЫЕ ПАССИВНЫЕ МЕХАНИКИ ==========

    private void applyBloodMoon(Player player, TalismanMechanic mechanic) {
        long time = player.getWorld().getTime();
        if (time >= 13000 && time <= 23000) { // Night time
            int strength = mechanic.getInt("strength_amplifier", 1);
            player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.STRENGTH, 100, strength));
        }
    }

    private void applySolarFlare(Player player, TalismanMechanic mechanic) {
        long time = player.getWorld().getTime();
        if (time < 13000 || time > 23000) { // Day time
            int resistance = mechanic.getInt("resistance_amplifier", 0);
            int regen = mechanic.getInt("regen_amplifier", 0);
            player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.RESISTANCE, 100, resistance));
            player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.REGENERATION, 100, regen));
        }
    }

    private void applyElementalImmunity(Player player, TalismanMechanic mechanic) {
        player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.FIRE_RESISTANCE, 100, 0));
    }

    private void applySpiritWalk(Player player, TalismanMechanic mechanic) {
        // Allows walking through mobs - handled via noClip mechanics
        player.setCollidable(false);
    }

    private void applyPoisonImmunity(Player player, TalismanMechanic mechanic) {
        if (player.hasPotionEffect(PotionEffectType.POISON)) {
            player.removePotionEffect(PotionEffectType.POISON);
        }
    }

    private void applyWitherImmunity(Player player, TalismanMechanic mechanic) {
        if (player.hasPotionEffect(PotionEffectType.WITHER)) {
            player.removePotionEffect(PotionEffectType.WITHER);
        }
    }

    private void applyKnockbackImmunity(Player player, TalismanMechanic mechanic) {
        player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.RESISTANCE, 100, 0));
    }

    private void applyFallDamageImmunity(Player player, TalismanMechanic mechanic) {
        player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.SLOW_FALLING, 40, 0));
    }

    private void applyExplosionImmunity(Player player, TalismanMechanic mechanic) {
        player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.RESISTANCE, 100, 2));
    }

    private void applyMagicBarrier(Player player, TalismanMechanic mechanic) {
        player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.RESISTANCE, 100, 1));
    }

    private void applyAngelWings(Player player, TalismanMechanic mechanic) {
        player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.LEVITATION, 10, 0));
        player.setAllowFlight(true);
    }

    private void applyDarkPact(Player player, TalismanMechanic mechanic) {
        double damage = mechanic.getDouble("health_drain", 0.5);
        if (player.getHealth() > damage) {
            player.damage(damage);
            int strength = mechanic.getInt("strength_amplifier", 2);
            player.addPotionEffect(createPotionEffect(mechanic, PotionEffectType.STRENGTH, 100, strength));
        }
    }

    private PotionEffect createPotionEffect(TalismanMechanic mechanic, PotionEffectType type, int duration, int amplifier) {
        TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
        boolean ambient = effect != null ? effect.ambient() : true;
        boolean particles = effect != null ? effect.particles() : false;
        boolean icon = effect != null ? effect.icon() : true;
        return new PotionEffect(type, duration, amplifier, ambient, particles, icon);
    }

    // ========== НОВЫЕ АТАКУЮЩИЕ МЕХАНИКИ ==========

    private void handlePhaseShift(Player attacker, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.20);
        if (Math.random() < chance) {
            double radius = mechanic.getDouble("radius", 5.0);
            Location current = attacker.getLocation();
            double angle = Math.random() * 2 * Math.PI;
            Location newLoc = current.clone().add(
                Math.cos(angle) * radius,
                0,
                Math.sin(angle) * radius
            );
            newLoc.setY(newLoc.getWorld().getHighestBlockYAt(newLoc));
            attacker.teleport(newLoc);
            spawnHitParticles(newLoc, Particle.PORTAL);
        }
    }

    private void handleVoidStrike(EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.15);
        if (Math.random() < chance) {
            double multiplier = mechanic.getDouble("multiplier", 2.0);
            event.setDamage(event.getDamage() * multiplier);
            spawnHitParticles(event.getEntity().getLocation(), Particle.DRAGON_BREATH);
        }
    }

    private void handleChaosDamage(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.25);
        if (Math.random() < chance) {
            Particle[] particles = {Particle.FLAME, Particle.SNOWFLAKE, Particle.ELECTRIC_SPARK, Particle.WITCH};
            Particle random = particles[(int) (Math.random() * particles.length)];
            spawnHitParticles(target.getLocation(), random);

            // Random elemental effect
            if (random == Particle.FLAME) {
                target.setFireTicks(60);
            } else if (random == Particle.SNOWFLAKE) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            } else if (random == Particle.ELECTRIC_SPARK) {
                target.getWorld().strikeLightningEffect(target.getLocation());
            } else {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1));
            }
        }
    }

    private void handleCurseOnHit(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.20);
        if (Math.random() < chance) {
            int duration = mechanic.getInt("duration", 200);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 0));
            spawnHitParticles(target.getLocation(), Particle.SOUL);
        }
    }

    private void handleHolySmite(LivingEntity target, EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        // Extra damage against undead
        String typeName = target.getType().name();
        if (typeName.contains("ZOMBIE") || typeName.contains("SKELETON") ||
            typeName.contains("WITHER") || typeName.equals("PHANTOM")) {
            double multiplier = mechanic.getDouble("multiplier", 2.0);
            event.setDamage(event.getDamage() * multiplier);
            spawnHitParticles(target.getLocation(), Particle.FIREWORK);
        }
    }

    private void handleComboDamage(Player attacker, EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        String key = "combo_hits";
        Map<String, Long> playerData = cooldowns.computeIfAbsent(attacker.getUniqueId(), k -> new HashMap<>());
        long lastHit = playerData.getOrDefault(key, 0L);
        long now = System.currentTimeMillis();

        if (now - lastHit < 2000) { // Within 2 seconds
            long comboCount = playerData.getOrDefault(key + "_count", 0L) + 1;
            playerData.put(key + "_count", comboCount);

            double bonusPerHit = mechanic.getDouble("bonus_per_hit", 0.10);
            double maxBonus = mechanic.getDouble("max_bonus", 1.0);
            double bonus = Math.min(comboCount * bonusPerHit, maxBonus);

            event.setDamage(event.getDamage() * (1 + bonus));
            attacker.sendMessage(Component.text("⚔ Комбо x" + comboCount).color(NamedTextColor.GOLD));
        } else {
            playerData.put(key + "_count", 0L);
        }

        playerData.put(key, now);
    }

    private void handleWhirlwind(Player attacker, LivingEntity target, EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.15);
        if (Math.random() < chance) {
            double radius = mechanic.getDouble("radius", 4.0);
            double damage = mechanic.getDouble("damage", event.getDamage() * 0.5);

            for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity living && entity != attacker) {
                    living.damage(damage, attacker);
                }
            }
            spawnHitParticles(target.getLocation(), Particle.SWEEP_ATTACK);
        }
    }

    private void handleCleave(Player attacker, LivingEntity target, EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        double radius = mechanic.getDouble("radius", 3.0);
        double damagePercent = mechanic.getDouble("damage_percent", 0.50);

        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity living && entity != attacker) {
                living.damage(event.getDamage() * damagePercent, attacker);
            }
        }
    }

    private void handleBleedOnHit(LivingEntity target, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.30);
        if (Math.random() < chance) {
            int duration = mechanic.getInt("duration", 100);
            int amplifier = mechanic.getInt("amplifier", 1);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, amplifier));
            spawnHitParticles(target.getLocation(), Particle.DAMAGE_INDICATOR);
        }
    }

    private void handleLifeTap(Player attacker, EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        double healthCost = mechanic.getDouble("health_cost", 2.0);
        if (attacker.getHealth() > healthCost) {
            attacker.damage(healthCost);
            double multiplier = mechanic.getDouble("damage_multiplier", 1.50);
            event.setDamage(event.getDamage() * multiplier);
            spawnHitParticles(attacker.getLocation(), Particle.DAMAGE_INDICATOR);
        }
    }

    private void handleExecute(LivingEntity target, EntityDamageByEntityEvent event, TalismanMechanic mechanic) {
        double threshold = mechanic.getDouble("health_threshold", 0.20);
        if (target.getHealth() < target.getMaxHealth() * threshold) {
            event.setDamage(target.getHealth() + 10); // Instant kill
            spawnHitParticles(target.getLocation(), Particle.SWEEP_ATTACK);
        }
    }

    // ========== НОВЫЕ ЗАЩИТНЫЕ МЕХАНИКИ ==========

    private void handleManaShield(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        int xpPerHeart = mechanic.getInt("xp_per_heart", 10);
        double damage = event.getDamage();
        int xpCost = (int) (damage * xpPerHeart);

        if (player.getTotalExperience() >= xpCost) {
            player.giveExp(-xpCost);
            event.setDamage(0);
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 20);
        }
    }

    private void handleReflectProjectiles(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            double chance = mechanic.getDouble("chance", 0.30);
            if (Math.random() < chance) {
                event.setCancelled(true);
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    private void handleTimeSlow(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        double chance = mechanic.getDouble("chance", 0.20);
        if (Math.random() < chance) {
            double radius = mechanic.getDouble("radius", 5.0);
            int duration = mechanic.getInt("duration", 100);

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity living) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 3));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 2));
                }
            }
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 100, 5, 5, 5);
        }
    }

    private void handleRuneShield(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        String cooldownKey = "rune_shield";
        if (!isOnCooldown(player, cooldownKey)) {
            int hearts = mechanic.getInt("absorption_hearts", 4);
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, hearts - 1));

            long cooldown = mechanic.getLong("cooldown", 30000);
            setCooldown(player, cooldownKey, cooldown);

            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 50);
        }
    }

    private void handleElementalAbsorption(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
            event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
            event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {

            double heal = event.getDamage() * mechanic.getDouble("heal_percent", 0.50);
            player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
            event.setCancelled(true);
            spawnHitParticles(player.getLocation(), Particle.HEART);
        }
    }

    private void handleSpectralForm(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        String cooldownKey = "spectral_form";
        if (!isOnCooldown(player, cooldownKey)) {
            event.setCancelled(true);

            int duration = mechanic.getInt("duration", 60);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));
            player.setCollidable(false);

            long cooldown = mechanic.getLong("cooldown", 60000);
            setCooldown(player, cooldownKey, cooldown);

            player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 30);
        }
    }

    private void handleFrostNova(Player player, TalismanMechanic mechanic) {
        String cooldownKey = "frost_nova";
        if (!isOnCooldown(player, cooldownKey)) {
            double radius = mechanic.getDouble("radius", 5.0);
            int duration = mechanic.getInt("duration", 100);

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity living) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 4));
                    living.setFreezeTicks(duration);
                }
            }

            player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 200, radius, 1, radius);

            long cooldown = mechanic.getLong("cooldown", 20000);
            setCooldown(player, cooldownKey, cooldown);
        }
    }

    private void handleShockWave(Player player, TalismanMechanic mechanic) {
        double radius = mechanic.getDouble("radius", 4.0);
        double damage = mechanic.getDouble("damage", 3.0);
        double knockback = mechanic.getDouble("knockback", 2.0);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity living) {
                living.damage(damage, player);
                org.bukkit.util.Vector direction = living.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
                living.setVelocity(direction.multiply(knockback));
            }
        }

        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, radius, 0.5, radius);
    }

    private void handleCounterAttack(Player player, EntityDamageEvent event, TalismanMechanic mechanic) {
        if (event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof LivingEntity attacker) {
            if (player.isBlocking()) {
                double damage = mechanic.getDouble("damage_multiplier", 1.50) * event.getDamage();
                attacker.damage(damage, player);
                event.setCancelled(true);
                spawnHitParticles(attacker.getLocation(), Particle.CRIT);
            }
        }
    }

    private void handleProjectileDeflect(EntityDamageEvent event, TalismanMechanic mechanic) {
        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            double chance = mechanic.getDouble("chance", 0.40);
            if (Math.random() < chance) {
                double reduction = mechanic.getDouble("damage_reduction", 0.50);
                event.setDamage(event.getDamage() * reduction);
            }
        }
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
