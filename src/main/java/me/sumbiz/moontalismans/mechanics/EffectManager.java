package me.sumbiz.moontalismans.mechanics;

import me.sumbiz.moontalismans.MoonTalismansPlugin;
import me.sumbiz.moontalismans.TalismanItem;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages passive effects, cooldowns, and active abilities for talismans and spheres.
 * All mechanic logic is delegated to {@link MechanicEngine}.
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

    // Cooldowns per player per ability
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    // Active effects per player
    private final Map<UUID, Set<String>> activeEffects = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastPassiveItemId = new ConcurrentHashMap<>();
    private final Map<UUID, Set<PotionEffectType>> lastPassivePotionEffects = new ConcurrentHashMap<>();
    private final Set<UUID> pandoraPotionExtensionInProgress = ConcurrentHashMap.newKeySet();

    // Scheduled tasks
    private BukkitRunnable passiveEffectTask;
    private BukkitRunnable particleTask;
    private int cooldownCleanupTaskId = -1;

    public EffectManager(MoonTalismansPlugin plugin) {
        this.plugin = plugin;
        this.keyId = new NamespacedKey(plugin, "talisman_id");
        this.keyType = new NamespacedKey(plugin, "talisman_type");
        this.mechanicEngine = new MechanicEngine(plugin);

        ConfigurationSection effectsSection = plugin.getConfig().getConfigurationSection("effects");

        passiveEffectsEnabled = effectsSection == null || effectsSection.getBoolean("passive_effects_enabled", true);
        particlesEnabled = effectsSection == null || effectsSection.getBoolean("particles_enabled", true);
        effectRefreshIntervalTicks = Math.max(1L, effectsSection != null
            ? effectsSection.getLong("effect_refresh_interval", 40L) : 40L);
        particleIntervalTicks = Math.max(1L, effectsSection != null
            ? effectsSection.getLong("particle_interval", 10L) : 10L);

        startPassiveEffectTask();
        startParticleTask();
        startCooldownCleanupTask();
    }

    // ========== SCHEDULED TASKS ==========

    private void startPassiveEffectTask() {
        if (!passiveEffectsEnabled) return;
        passiveEffectTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyPassiveEffects(player);
                }
            }
        };
        passiveEffectTask.runTaskTimer(plugin, 20L, effectRefreshIntervalTicks);
    }

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

    /**
     * Async task that periodically cleans up expired cooldowns to prevent memory growth.
     */
    private void startCooldownCleanupTask() {
        cooldownCleanupTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            cooldowns.forEach((uuid, map) -> {
                map.entrySet().removeIf(entry -> entry.getValue() < now);
                if (map.isEmpty()) cooldowns.remove(uuid);
            });
            mechanicEngine.cleanExpiredCooldowns(now);
        }, 6000L, 6000L).getTaskId(); // Every 5 minutes
    }

    // ========== PASSIVE EFFECTS ==========

    private void applyPassiveEffects(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);

        if (itemId == null) {
            clearPassiveEffects(player);
            activeEffects.remove(player.getUniqueId());
            lastPassiveItemId.remove(player.getUniqueId());
            lastPassivePotionEffects.remove(player.getUniqueId());
            return;
        }

        String previousItemId = lastPassiveItemId.get(player.getUniqueId());
        if (previousItemId != null && !previousItemId.equals(itemId)) {
            clearPassiveEffects(player);
            lastPassivePotionEffects.remove(player.getUniqueId());
        }

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) {
            clearPassiveEffects(player);
            activeEffects.remove(player.getUniqueId());
            lastPassiveItemId.remove(player.getUniqueId());
            lastPassivePotionEffects.remove(player.getUniqueId());
            return;
        }

        TalismanItem item = itemOpt.get();
        lastPassiveItemId.put(player.getUniqueId(), itemId);
        lastPassivePotionEffects.put(player.getUniqueId(), collectPassivePotionEffects(item));

        if (!passiveEffectsEnabled) {
            return;
        }

        // Apply configured passive potion effects from item definition
        applyConfiguredPassivePotions(player, item);

        // Apply mechanic system passive effects
        mechanicEngine.applyPassiveMechanics(player, item);
    }

    private void applyConfiguredPassivePotions(Player player, TalismanItem item) {
        for (TalismanItem.ConfiguredPotionEffect effect : item.getPassivePotionEffects()) {
            player.addPotionEffect(effect.toPotionEffect());
        }
    }

    private void clearPassiveEffects(Player player) {
        Set<PotionEffectType> previous = lastPassivePotionEffects.get(player.getUniqueId());
        if (previous == null || previous.isEmpty()) {
            return;
        }
        for (PotionEffectType type : previous) {
            player.removePotionEffect(type);
        }
    }

    private Set<PotionEffectType> collectPassivePotionEffects(TalismanItem item) {
        Set<PotionEffectType> effects = new HashSet<>();
        for (TalismanItem.ConfiguredPotionEffect effect : item.getPassivePotionEffects()) {
            effects.add(effect.type());
        }

        for (TalismanMechanic mechanic : item.getMechanics()) {
            if (!mechanic.isEnabled() || !mechanic.getType().isPassive()) {
                continue;
            }
            TalismanMechanic.PotionEffectConfig effect = mechanic.getPotionEffect("effect");
            if (effect != null) {
                effects.add(effect.type());
            }
            switch (mechanic.getType()) {
                case WATER_BREATHING -> effects.add(PotionEffectType.DOLPHINS_GRACE);
                case ENHANCED_JUMP -> effects.add(PotionEffectType.JUMP_BOOST);
                case INVISIBILITY_ON_SNEAK -> effects.add(PotionEffectType.INVISIBILITY);
                case ELEMENTAL_IMMUNITY -> effects.add(PotionEffectType.FIRE_RESISTANCE);
                case KNOCKBACK_IMMUNITY, MAGIC_BARRIER, EXPLOSION_IMMUNITY -> effects.add(PotionEffectType.RESISTANCE);
                case FALL_DAMAGE_IMMUNITY -> effects.add(PotionEffectType.SLOW_FALLING);
                case ANGEL_WINGS -> effects.add(PotionEffectType.LEVITATION);
                case DARK_PACT, BLOOD_MOON -> effects.add(PotionEffectType.STRENGTH);
                case SOLAR_FLARE -> {
                    effects.add(PotionEffectType.RESISTANCE);
                    effects.add(PotionEffectType.REGENERATION);
                }
                default -> {}
            }
        }

        return effects;
    }

    // ========== PARTICLES ==========

    private void spawnAmbientParticles(Player player) {
        if (!particlesEnabled) return;
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);

        if (itemId == null) return;

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) return;

        TalismanItem item = itemOpt.get();
        if (item.isSphere()) {
            Location loc = player.getLocation().clone().add(0, 1, 0);
            World world = player.getWorld();
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

    // ========== EVENT HANDLERS ==========

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
        mechanicEngine.handleAttackMechanics(player, target, event, item);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        ItemStack offhand = killer.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);
        if (itemId == null) return;

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) return;

        TalismanItem item = itemOpt.get();
        for (TalismanMechanic mechanic : item.getMechanics()) {
            if (!mechanic.isEnabled() || mechanic.getType() != MechanicType.HEAL_ON_KILL) {
                continue;
            }

            double heal = mechanic.getDouble("heal", 4.0);
            if (heal <= 0) {
                continue;
            }

            killer.setHealth(Math.min(killer.getHealth() + heal, killer.getMaxHealth()));
            spawnHitParticles(killer.getLocation(), Particle.HEART);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getNewEffect() == null) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED
            && event.getAction() != EntityPotionEffectEvent.Action.CHANGED) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (pandoraPotionExtensionInProgress.remove(playerId)) return;

        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);
        if (itemId == null) return;

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) return;

        TalismanItem item = itemOpt.get();
        PandoraPotionSettings settings = getPandoraPotionSettings(item);
        if (settings == null || !settings.enabled()) return;
        if (settings.chance() <= 0 || settings.multiplier() <= 1.0) return;
        if (ThreadLocalRandom.current().nextDouble() >= settings.chance()) return;

        PotionEffect newEffect = event.getNewEffect();
        int duration = newEffect.getDuration();
        if (duration <= 0) return;

        long extendedDuration = Math.round(duration * settings.multiplier());
        int cappedDuration = (int) Math.min(Integer.MAX_VALUE, extendedDuration);
        if (cappedDuration == duration) return;

        event.setCancelled(true);
        pandoraPotionExtensionInProgress.add(playerId);
        player.addPotionEffect(new PotionEffect(
            newEffect.getType(),
            cappedDuration,
            newEffect.getAmplifier(),
            newEffect.isAmbient(),
            newEffect.hasParticles(),
            newEffect.hasIcon()
        ));
    }

    private PandoraPotionSettings getPandoraPotionSettings(TalismanItem item) {
        for (TalismanMechanic mechanic : item.getMechanics()) {
            if (mechanic.getType() == MechanicType.PANDORA_POTION_EXTENSION) {
                double chance = mechanic.getDouble("chance", 0.5);
                double multiplier = mechanic.getDouble("multiplier", 2.0);
                return new PandoraPotionSettings(mechanic.isEnabled(), chance, multiplier);
            }
        }
        return null;
    }

    private record PandoraPotionSettings(boolean enabled, double chance, double multiplier) {}

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);
        if (itemId == null) return;

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) return;

        TalismanItem item = itemOpt.get();

        // Check death mechanics first
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            if (mechanicEngine.handleDeathMechanics(player, event, item)) {
                event.setCancelled(true);
                return;
            }
        }

        // Apply damage mechanics
        mechanicEngine.handleDamageMechanics(player, event, item);
    }

    @EventHandler
    public void onItemSwap(PlayerSwapHandItemsEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyPassiveEffects(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyPassiveEffects(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clearPassiveEffects(event.getPlayer());
        cooldowns.remove(uuid);
        activeEffects.remove(uuid);
        lastPassiveItemId.remove(uuid);
        lastPassivePotionEffects.remove(uuid);
        pandoraPotionExtensionInProgress.remove(uuid);
        mechanicEngine.clearPlayerData(uuid);
    }

    // ========== UTILITIES ==========

    private void spawnHitParticles(Location loc, Particle particle) {
        Location adjusted = loc.clone().add(0, 1, 0);
        adjusted.getWorld().spawnParticle(particle, adjusted, 10, 0.3, 0.3, 0.3, 0.05);
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
        if (passiveEffectTask != null) {
            passiveEffectTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
        if (cooldownCleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cooldownCleanupTaskId);
        }
        mechanicEngine.cleanup();
        cooldowns.clear();
        activeEffects.clear();
        lastPassiveItemId.clear();
        lastPassivePotionEffects.clear();
        pandoraPotionExtensionInProgress.clear();
    }
}
