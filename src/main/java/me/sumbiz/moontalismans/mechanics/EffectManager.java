package me.sumbiz.moontalismans.mechanics;

import me.sumbiz.moontalismans.MoonTalismansPlugin;
import me.sumbiz.moontalismans.TalismanItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages passive effects, cooldowns, and active abilities for talismans and spheres.
 */
public class EffectManager implements Listener {

    private final MoonTalismansPlugin plugin;
    private final NamespacedKey keyId;
    private final NamespacedKey keyType;
    private final MechanicEngine mechanicEngine;

    private final boolean passiveEffectsEnabled;
    private final boolean particlesEnabled;
    private final long effectRefreshIntervalTicks;
    private final long particleIntervalTicks;

    private final boolean phoenixEnabled;
    private final boolean healerEnabled;
    private final boolean tritonEnabled;
    private final boolean graniEnabled;
    private final boolean aegisEnabled;
    private final boolean magmaEnabled;
    private final boolean athenaEnabled;
    private final boolean theurgyEnabled;
    private final boolean iasoEnabled;
    private final boolean cobraEnabled;
    private final boolean echidnaEnabled;
    private final boolean punisherEnabled;
    private final boolean kraitEnabled;
    private final boolean crusherEnabled;
    private final boolean chimeraEnabled;
    private final boolean pandoraEnabled;
    private final boolean titanEnabled;
    private final boolean osirisEnabled;

    private final double phoenixLowHealthThreshold;
    private final double phoenixReviveHealthMultiplier;
    private final long phoenixCooldownMs;

    private final PotionSetting phoenixLowHealthRegen;
    private final PotionSetting phoenixReviveRegen;
    private final PotionSetting phoenixReviveFireResistance;
    private final PotionSetting healerRegeneration;
    private final PotionSetting tritonBreath;
    private final PotionSetting graniCombatSpeed;
    private final PotionSetting graniOnDamageSpeed;
    private final PotionSetting aegisResistance;
    private final PotionSetting magmaResistance;
    private final PotionSetting athenaStrength;
    private final PotionSetting theurgyAbsorption;
    private final PotionSetting iasoSaturation;
    private final PotionSetting cobraPoison;
    private final PotionSetting kraitSlow;
    private final PotionSetting crusherMiningFatigue;
    private final PotionSetting crusherNausea;
    private final PotionSetting chimeraEffectDuration;
    private final PotionSetting pandoraWeakness;
    private final PotionSetting titanResistance;

    private final double cobraPoisonChance;
    private final double echidnaLifesteal;
    private final double punisherCritBonus;
    private final double kraitSlowChance;
    private final double crusherStunChance;
    private final double chimeraRandomChance;
    private final double pandoraAoeChance;
    private final double aegisDamageReduction;
    private final double osirisReflect;
    private final double athenaHealthThreshold;
    private final double pandoraRadius;

    // Cooldowns per player per item
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    // Active effects per player
    private final Map<UUID, Set<String>> activeEffects = new ConcurrentHashMap<>();

    // Particle task
    private BukkitRunnable particleTask;

    public EffectManager(MoonTalismansPlugin plugin) {
        this.plugin = plugin;
        this.keyId = new NamespacedKey(plugin, "talisman_id");
        this.keyType = new NamespacedKey(plugin, "talisman_type");
        this.mechanicEngine = new MechanicEngine(plugin);

        ConfigurationSection effectsSection = plugin.getConfig().getConfigurationSection("effects");
        ConfigurationSection mechanicsSection = effectsSection != null
            ? effectsSection.getConfigurationSection("mechanics") : null;

        passiveEffectsEnabled = effectsSection == null || effectsSection.getBoolean("passive_effects_enabled", true);
        particlesEnabled = effectsSection == null || effectsSection.getBoolean("particles_enabled", true);
        effectRefreshIntervalTicks = Math.max(1L, effectsSection != null
            ? effectsSection.getLong("effect_refresh_interval", 40L) : 40L);
        particleIntervalTicks = Math.max(1L, effectsSection != null
            ? effectsSection.getLong("particle_interval", 10L) : 10L);

        phoenixEnabled = isMechanicEnabled(mechanicsSection, "phoenix", false);
        healerEnabled = isMechanicEnabled(mechanicsSection, "healer", false);
        tritonEnabled = isMechanicEnabled(mechanicsSection, "triton", false);
        graniEnabled = isMechanicEnabled(mechanicsSection, "grani", false);
        aegisEnabled = isMechanicEnabled(mechanicsSection, "aegis", false);
        magmaEnabled = isMechanicEnabled(mechanicsSection, "magma", false);
        athenaEnabled = isMechanicEnabled(mechanicsSection, "athena", false);
        theurgyEnabled = isMechanicEnabled(mechanicsSection, "theurgy", false);
        iasoEnabled = isMechanicEnabled(mechanicsSection, "iaso", false);
        cobraEnabled = isMechanicEnabled(mechanicsSection, "cobra", false);
        echidnaEnabled = isMechanicEnabled(mechanicsSection, "echidna", false);
        punisherEnabled = isMechanicEnabled(mechanicsSection, "punisher", false);
        kraitEnabled = isMechanicEnabled(mechanicsSection, "krait", false);
        crusherEnabled = isMechanicEnabled(mechanicsSection, "crusher", false);
        chimeraEnabled = isMechanicEnabled(mechanicsSection, "chimera", false);
        pandoraEnabled = isMechanicEnabled(mechanicsSection, "pandora", false);
        titanEnabled = isMechanicEnabled(mechanicsSection, "titan", false);
        osirisEnabled = isMechanicEnabled(mechanicsSection, "osiris", false);

        phoenixLowHealthThreshold = readDouble(mechanicsSection, "phoenix.low_health_threshold", 0.3);
        phoenixReviveHealthMultiplier = readDouble(mechanicsSection, "phoenix.revive.health_multiplier", 0.3);
        phoenixCooldownMs = readLong(mechanicsSection, "phoenix.cooldown",
            effectsSection != null ? effectsSection.getLong("phoenix_cooldown", 300000L) : 300000L);

        phoenixLowHealthRegen = readPotion(mechanicsSection, "phoenix.low_health_regeneration", 60, 1);
        phoenixReviveRegen = readPotion(mechanicsSection, "phoenix.revive.regeneration", 100, 2);
        phoenixReviveFireResistance = readPotion(mechanicsSection, "phoenix.revive.fire_resistance", 100, 0);
        healerRegeneration = readPotion(mechanicsSection, "healer.regeneration", 60, 0);
        tritonBreath = readPotion(mechanicsSection, "triton.water_breathing", 100, 0);
        graniCombatSpeed = readPotion(mechanicsSection, "grani.combat_speed", 60, 0);
        graniOnDamageSpeed = readPotion(mechanicsSection, "grani.on_damage_speed", 60, 1);
        aegisResistance = readPotion(mechanicsSection, "aegis.resistance", 60, 0);
        magmaResistance = readPotion(mechanicsSection, "magma.fire_resistance", 60, 0);
        athenaStrength = readPotion(mechanicsSection, "athena.strength", 60, 0);
        theurgyAbsorption = readPotion(mechanicsSection, "theurgy.absorption", 200, 0);
        iasoSaturation = readPotion(mechanicsSection, "iaso.saturation", 60, 0);
        cobraPoison = readPotion(mechanicsSection, "cobra.poison", 60, 0);
        kraitSlow = readPotion(mechanicsSection, "krait.slowness", 40, 1);
        crusherMiningFatigue = readPotion(mechanicsSection, "crusher.mining_fatigue", 60, 2);
        crusherNausea = readPotion(mechanicsSection, "crusher.nausea", 60, 0);
        chimeraEffectDuration = readPotion(mechanicsSection, "chimera.effect", 60, 0);
        pandoraWeakness = readPotion(mechanicsSection, "pandora.weakness", 60, 0);
        titanResistance = readPotion(mechanicsSection, "titan.resistance", 40, 0);

        cobraPoisonChance = readDouble(mechanicsSection, "cobra.chance",
            readLegacyDouble(effectsSection, "chances.cobra_poison", 0.30));
        echidnaLifesteal = readDouble(mechanicsSection, "echidna.lifesteal",
            readLegacyDouble(effectsSection, "multipliers.echidna_lifesteal", 0.15));
        punisherCritBonus = readDouble(mechanicsSection, "punisher.crit_bonus",
            readLegacyDouble(effectsSection, "multipliers.punisher_crit_bonus", 1.20));
        kraitSlowChance = readDouble(mechanicsSection, "krait.chance",
            readLegacyDouble(effectsSection, "chances.krait_slow", 0.25));
        crusherStunChance = readDouble(mechanicsSection, "crusher.chance",
            readLegacyDouble(effectsSection, "chances.crusher_stun", 0.20));
        chimeraRandomChance = readDouble(mechanicsSection, "chimera.chance",
            readLegacyDouble(effectsSection, "chances.chimera_random", 0.35));
        pandoraAoeChance = readDouble(mechanicsSection, "pandora.chance",
            readLegacyDouble(effectsSection, "chances.pandora_aoe", 0.25));
        aegisDamageReduction = readDouble(mechanicsSection, "aegis.damage_reduction",
            readLegacyDouble(effectsSection, "multipliers.aegis_damage_reduction", 0.90));
        osirisReflect = readDouble(mechanicsSection, "osiris.reflect",
            readLegacyDouble(effectsSection, "multipliers.osiris_reflect", 0.15));
        athenaHealthThreshold = readDouble(mechanicsSection, "athena.health_threshold", 0.5);
        pandoraRadius = readDouble(mechanicsSection, "pandora.radius", 3.0);

        startPassiveEffectTask();
        startParticleTask();
    }

    private boolean isMechanicEnabled(ConfigurationSection mechanics, String path, boolean defaultValue) {
        if (mechanics == null) return defaultValue;
        ConfigurationSection section = mechanics.getConfigurationSection(path);
        if (section != null && section.isSet("enabled")) {
            return section.getBoolean("enabled");
        }
        return defaultValue;
    }

    private PotionSetting readPotion(ConfigurationSection mechanics, String path, int defaultDuration, int defaultAmplifier) {
        if (mechanics == null) return new PotionSetting(defaultDuration, defaultAmplifier);
        ConfigurationSection section = mechanics.getConfigurationSection(path);
        if (section == null) return new PotionSetting(defaultDuration, defaultAmplifier);
        int duration = section.getInt("duration", defaultDuration);
        int amplifier = section.getInt("amplifier", defaultAmplifier);
        return new PotionSetting(duration, amplifier);
    }

    private double readDouble(ConfigurationSection mechanics, String path, double defaultValue) {
        if (mechanics != null && mechanics.isSet(path)) {
            return mechanics.getDouble(path, defaultValue);
        }
        return defaultValue;
    }

    private double readLegacyDouble(ConfigurationSection effects, String path, double defaultValue) {
        if (effects != null && effects.isSet(path)) {
            return effects.getDouble(path, defaultValue);
        }
        return defaultValue;
    }

    private long readLong(ConfigurationSection mechanics, String path, long defaultValue) {
        if (mechanics != null && mechanics.isSet(path)) {
            return mechanics.getLong(path, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Task that applies passive potion effects every 2 seconds.
     */
    private void startPassiveEffectTask() {
        if (!passiveEffectsEnabled) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyPassiveEffects(player);
                }
            }
        }.runTaskTimer(plugin, 20L, effectRefreshIntervalTicks);
    }

    /**
     * Task that spawns particles around players with special items.
     */
    private void startParticleTask() {
        if (!particlesEnabled) return;
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    spawnAmbientParticles(player);
                }
            }
        };
        particleTask.runTaskTimer(plugin, 10L, particleIntervalTicks);
    }

    private void applyPassiveEffects(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);

        if (itemId == null) {
            // Clear effects from this player
            activeEffects.remove(player.getUniqueId());
            return;
        }

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) return;

        TalismanItem item = itemOpt.get();

        // Apply built-in passive effects based on item ID patterns
        applyBuiltInPassiveEffects(player, item);

        // Apply new mechanic system
        mechanicEngine.applyPassiveMechanics(player, item);
    }

    /**
     * Apply passive effects based on talisman/sphere type.
     */
    private void applyBuiltInPassiveEffects(Player player, TalismanItem item) {
        String id = item.getId().toLowerCase();

        if (!passiveEffectsEnabled) {
            return;
        }

        applyConfiguredPassivePotions(player, item);

        // Феникс - регенерация при низком здоровье
        if (phoenixEnabled && (id.contains("feniksa") || id.contains("phoenix"))) {
            if (player.getHealth() < player.getMaxHealth() * phoenixLowHealthThreshold) {
                addPotion(player, PotionEffectType.REGENERATION, phoenixLowHealthRegen, true, false, true);
            }
        }

        // Лекарь - постоянная медленная регенерация
        if (healerEnabled && (id.contains("lekarya") || id.contains("healer"))) {
            addPotion(player, PotionEffectType.REGENERATION, healerRegeneration, true, false, true);
        }

        // Тритон - водное дыхание
        if (tritonEnabled && (id.contains("tritona") || id.contains("triton"))) {
            if (player.isInWater()) {
                addPotion(player, PotionEffectType.WATER_BREATHING, tritonBreath, true, false, true);
                addPotion(player, PotionEffectType.DOLPHINS_GRACE, tritonBreath, true, false, true);
            }
        }

        // Грани - ускорение в бою
        if (graniEnabled && (id.contains("grani") || id.contains("grani"))) {
            if (player.getLastDamage() > 0 && (System.currentTimeMillis() - player.getLastDamageCause().getEntity().getTicksLived()) < 100) {
                addPotion(player, PotionEffectType.SPEED, graniCombatSpeed, true, false, true);
            }
        }

        // Эгида - сопротивление урону
        if (aegisEnabled && (id.contains("egida") || id.contains("aegis"))) {
            addPotion(player, PotionEffectType.RESISTANCE, aegisResistance, true, false, true);
        }

        // Магма - огнестойкость
        if (magmaEnabled && id.contains("magma")) {
            addPotion(player, PotionEffectType.FIRE_RESISTANCE, magmaResistance, true, false, true);
        }

        // Афина - боевая ярость
        if (athenaEnabled && (id.contains("afina") || id.contains("athena"))) {
            // Gives strength when health drops below 50%
            if (player.getHealth() < player.getMaxHealth() * athenaHealthThreshold) {
                addPotion(player, PotionEffectType.STRENGTH, athenaStrength, true, false, true);
            }
        }

        // Теургия - дополнительное поглощение
        if (theurgyEnabled && (id.contains("teurgia") || id.contains("theurgy"))) {
            if (!player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
                addPotion(player, PotionEffectType.ABSORPTION, theurgyAbsorption, true, false, true);
            }
        }

        // Иасо - усиленное исцеление
        if (iasoEnabled && id.contains("iaso")) {
            addPotion(player, PotionEffectType.SATURATION, iasoSaturation, true, false, true);
        }
    }

    private void addPotion(LivingEntity entity, PotionEffectType type, PotionSetting setting, boolean ambient, boolean particles, boolean icon) {
        entity.addPotionEffect(new PotionEffect(type, setting.durationTicks(), setting.amplifier(), ambient, particles, icon));
    }

    private void applyConfiguredPassivePotions(Player player, TalismanItem item) {
        for (TalismanItem.ConfiguredPotionEffect effect : item.getPassivePotionEffects()) {
            player.addPotionEffect(effect.toPotionEffect());
        }
    }

    /**
     * Spawn ambient particles around players with special items.
     */
    private void spawnAmbientParticles(Player player) {
        if (!particlesEnabled) return;
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);

        if (itemId == null) return;

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) return;

        TalismanItem item = itemOpt.get();
        String id = item.getId().toLowerCase();

        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        // Different particles based on item type
        if (id.contains("feniksa") || id.contains("phoenix")) {
            spawnCircleParticles(world, loc, Particle.FLAME, 3, 0.3);
        } else if (id.contains("magma")) {
            spawnCircleParticles(world, loc, Particle.LAVA, 2, 0.2);
        } else if (id.contains("tritona") || id.contains("triton")) {
            if (player.isInWater()) {
                spawnCircleParticles(world, loc, Particle.BUBBLE_COLUMN_UP, 5, 0.4);
            }
        } else if (id.contains("chimera") || id.contains("himera")) {
            spawnCircleParticles(world, loc, Particle.ENCHANT, 5, 0.5);
        } else if (id.contains("apollon") || id.contains("apollo")) {
            spawnCircleParticles(world, loc, Particle.END_ROD, 2, 0.4);
        } else if (id.contains("andromeda")) {
            spawnCircleParticles(world, loc, Particle.PORTAL, 3, 0.3);
        } else if (id.contains("pandora")) {
            spawnCircleParticles(world, loc, Particle.WITCH, 2, 0.3);
        } else if (id.contains("titan")) {
            spawnCircleParticles(world, loc, Particle.CRIT, 3, 0.4);
        } else if (item.isSphere()) {
            // Default sphere particles
            spawnCircleParticles(world, loc, Particle.DUST, 2, 0.25);
        }
    }

    private void spawnCircleParticles(World world, Location center, Particle particle, int count, double radius) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            if (particle == Particle.DUST) {
                world.spawnParticle(particle, x, center.getY(), z, 1,
                    new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.0f));
            } else {
                world.spawnParticle(particle, x, center.getY(), z, 1, 0, 0, 0, 0);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);

        if (itemId == null) return;

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) return;

        TalismanItem item = itemOpt.get();
        String id = item.getId().toLowerCase();

        // Кобра - отравление при ударе
        if (cobraEnabled && (id.contains("kobry") || id.contains("cobra"))) {
            if (Math.random() < cobraPoisonChance) {
                addPotion(target, PotionEffectType.POISON, cobraPoison, true, true, true);
                spawnHitParticles(target.getLocation(), Particle.WITCH);
            }
        }

        // Ехидна - высасывание жизни
        if (echidnaEnabled && (id.contains("ekhidny") || id.contains("echidna"))) {
            double heal = event.getDamage() * echidnaLifesteal;
            player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
            spawnHitParticles(player.getLocation(), Particle.HEART);
        }

        // Каратель - дополнительный урон при критическом ударе
        if (punisherEnabled && (id.contains("karatelya") || id.contains("punisher"))) {
            if (player.getFallDistance() > 0 && !player.isOnGround()) {
                event.setDamage(event.getDamage() * punisherCritBonus);
                spawnHitParticles(target.getLocation(), Particle.ENCHANTED_HIT);
            }
        }

        // Крайт - замедление при ударе
        if (kraitEnabled && (id.contains("kraita") || id.contains("krait"))) {
            if (Math.random() < kraitSlowChance) {
                addPotion(target, PotionEffectType.SLOWNESS, kraitSlow, true, true, true);
                spawnHitParticles(target.getLocation(), Particle.SNOWFLAKE);
            }
        }

        // Крушитель - оглушение при ударе
        if (crusherEnabled && (id.contains("krushitelya") || id.contains("crusher"))) {
            if (Math.random() < crusherStunChance) {
                addPotion(target, PotionEffectType.MINING_FATIGUE, crusherMiningFatigue, true, true, true);
                addPotion(target, PotionEffectType.NAUSEA, crusherNausea, true, true, true);
                spawnHitParticles(target.getLocation(), Particle.SONIC_BOOM);
            }
        }

        // Химера - случайные эффекты
        if (chimeraEnabled && (id.contains("chimera") || id.contains("himera"))) {
            if (Math.random() < chimeraRandomChance) {
                applyRandomChimeraEffect(target);
            }
        }

        // Пандора - распространение эффектов
        if (pandoraEnabled && id.contains("pandora")) {
            if (Math.random() < pandoraAoeChance) {
                for (Entity nearby : target.getNearbyEntities(pandoraRadius, pandoraRadius, pandoraRadius)) {
                    if (nearby instanceof LivingEntity le && nearby != player) {
                        addPotion(le, PotionEffectType.WEAKNESS, pandoraWeakness, true, true, true);
                    }
                }
                spawnHitParticles(target.getLocation(), Particle.SCULK_CHARGE_POP);
            }
        }

        // Apply new mechanic system
        mechanicEngine.handleAttackMechanics(player, target, event, item);
    }

    private void applyRandomChimeraEffect(LivingEntity target) {
        PotionEffectType[] effects = {
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.BLINDNESS
        };
        PotionEffectType effect = effects[(int) (Math.random() * effects.length)];
        addPotion(target, effect, chimeraEffectDuration, true, true, true);

        Particle[] particles = {
            Particle.WITCH, Particle.SOUL, Particle.ENCHANT
        };
        spawnHitParticles(target.getLocation(), particles[(int) (Math.random() * particles.length)]);
    }

    private void spawnHitParticles(Location loc, Particle particle) {
        loc.getWorld().spawnParticle(particle, loc.add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);

        if (itemId == null) return;

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) return;

        TalismanItem item = itemOpt.get();
        String id = item.getId().toLowerCase();

        // Эгида - сокращение урона
        if (aegisEnabled && (id.contains("egida") || id.contains("aegis"))) {
            event.setDamage(event.getDamage() * aegisDamageReduction);
        }

        // Грани - ускорение при получении урона
        if (graniEnabled && id.contains("grani")) {
            addPotion(player, PotionEffectType.SPEED, graniOnDamageSpeed, true, false, true);
        }

        // Титан - устойчивость к отбрасыванию (дополнительная)
        if (titanEnabled && id.contains("titan")) {
            // The knockback resistance is already in attributes, but we add extra effect
            addPotion(player, PotionEffectType.RESISTANCE, titanResistance, true, false, true);
        }

        // Феникс - второй шанс
        if (phoenixEnabled && (id.contains("feniksa") || id.contains("phoenix"))) {
            if (player.getHealth() - event.getFinalDamage() <= 0) {
                if (!isOnCooldown(player, "phoenix_revive")) {
                    event.setCancelled(true);
                    player.setHealth(player.getMaxHealth() * phoenixReviveHealthMultiplier);
                    addPotion(player, PotionEffectType.REGENERATION, phoenixReviveRegen, true, false, true);
                    addPotion(player, PotionEffectType.FIRE_RESISTANCE, phoenixReviveFireResistance, true, false, true);

                    // Spawn revival effect
                    Location loc = player.getLocation();
                    player.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                    player.getWorld().spawnParticle(Particle.LAVA, loc, 20, 0.5, 0.5, 0.5, 0);
                    player.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);

                    setCooldown(player, "phoenix_revive", phoenixCooldownMs);
                    player.sendMessage(Component.text("⚔ ").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
                            .append(Component.text("Талисман Феникса спас вас от смерти! ").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                            .append(Component.text("(Кулдаун: 5 минут)").color(NamedTextColor.GRAY)));
                }
            }
        }

        // Check death mechanics from new system
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            if (mechanicEngine.handleDeathMechanics(player, event, item)) {
                event.setCancelled(true);
                return;
            }
        }

        // Apply damage mechanics from new system
        mechanicEngine.handleDamageMechanics(player, event, item);

        if (osirisEnabled) {
            double reflectMultiplier = item.getDamageReflect().orElse(osirisReflect);
            if (reflectMultiplier > 0 && event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof LivingEntity attacker) {
                attacker.damage(event.getDamage() * reflectMultiplier, player);
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    @EventHandler
    public void onItemSwap(PlayerSwapHandItemsEvent event) {
        // Refresh effects when swapping items
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyPassiveEffects(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        // Refresh effects when changing held item
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyPassiveEffects(event.getPlayer()), 1L);
    }

    private String getItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String storedId = pdc.get(keyId, PersistentDataType.STRING);
        if (storedId != null) {
            return storedId;
        }

        if (!meta.hasCustomModelData()) {
            return null;
        }

        int modelData = meta.getCustomModelData();
        Material material = item.getType();

        for (TalismanItem talisman : plugin.getItemManager().getItems().values()) {
            Integer talismanModelData = talisman.getCustomModelData();
            if (talismanModelData != null
                && talismanModelData == modelData
                && talisman.getMaterial() == material) {
                return talisman.getId();
            }
        }

        return null;
    }

    private boolean isOnCooldown(Player player, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Long expiry = playerCooldowns.get(ability);
        if (expiry == null) return false;

        return System.currentTimeMillis() < expiry;
    }

    private void setCooldown(Player player, String ability, long durationMs) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
            .put(ability, System.currentTimeMillis() + durationMs);
    }

    public long getRemainingCooldown(Player player, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long expiry = playerCooldowns.get(ability);
        if (expiry == null) return 0;

        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void shutdown() {
        if (particleTask != null) {
            particleTask.cancel();
        }
        mechanicEngine.cleanup();
        cooldowns.clear();
        activeEffects.clear();
    }

    private record PotionSetting(int durationTicks, int amplifier) {}
}
