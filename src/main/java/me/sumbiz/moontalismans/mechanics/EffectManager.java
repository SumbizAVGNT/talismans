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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages passive effects, cooldowns, and active abilities for talismans and spheres.
 * Uses batched processing and async computation to minimize main-thread load.
 * All mechanic logic is delegated to {@link MechanicEngine}.
 */
public class EffectManager implements Listener {

    private static final int PASSIVE_BATCH_DIVISOR = 4;

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

    // Player-Item cache: avoids repeated PDC lookups in event handlers
    private final Map<UUID, CachedPlayerItem> playerItemCache = new ConcurrentHashMap<>();

    // Deferred particle queue: particles computed during mechanics, flushed async
    private final ConcurrentLinkedQueue<DeferredParticle> particleQueue = new ConcurrentLinkedQueue<>();

    // Scheduled tasks
    private BukkitRunnable passiveEffectTask;
    private BukkitRunnable particleTask;
    private int cooldownCleanupTaskId = -1;
    private int particleFlushTaskId = -1;

    // Batched passive processing state
    private int passiveBatchIndex = 0;

    public EffectManager(MoonTalismansPlugin plugin) {
        this.plugin = plugin;
        this.keyId = new NamespacedKey(plugin, "talisman_id");
        this.keyType = new NamespacedKey(plugin, "talisman_type");
        this.mechanicEngine = new MechanicEngine(plugin);
        this.mechanicEngine.setParticleConsumer((loc, particle) ->
            queueParticle(loc.clone().add(0, 1, 0), particle, 10, 0.3, 0.3, 0.3, 0.05));

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
        startParticleFlushTask();
    }

    // ========== RECORDS ==========

    private record CachedPlayerItem(String itemId, TalismanItem item, long cachedAt) {
        boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > 1000; // 1 second TTL
        }
    }

    private record DeferredParticle(World world, double x, double y, double z,
                                     Particle particle, int count,
                                     double offsetX, double offsetY, double offsetZ,
                                     double extra, Object data) {}

    private record PandoraPotionSettings(boolean enabled, double chance, double multiplier) {}

    // ========== SCHEDULED TASKS ==========

    /**
     * Batched passive effect processing: instead of processing ALL players every N ticks,
     * processes 1/BATCH_DIVISOR of players per tick at a higher frequency.
     * This spreads the CPU load across multiple ticks for smoother performance.
     */
    private void startPassiveEffectTask() {
        if (!passiveEffectsEnabled) return;
        long batchInterval = Math.max(1L, effectRefreshIntervalTicks / PASSIVE_BATCH_DIVISOR);
        passiveEffectTask = new BukkitRunnable() {
            @Override
            public void run() {
                List<? extends Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (players.isEmpty()) return;

                int totalPlayers = players.size();
                int batchSize = Math.max(1, (totalPlayers + PASSIVE_BATCH_DIVISOR - 1) / PASSIVE_BATCH_DIVISOR);

                if (passiveBatchIndex >= totalPlayers) {
                    passiveBatchIndex = 0;
                }

                int end = Math.min(passiveBatchIndex + batchSize, totalPlayers);
                for (int i = passiveBatchIndex; i < end; i++) {
                    applyPassiveEffects(players.get(i));
                }
                passiveBatchIndex = end;
            }
        };
        passiveEffectTask.runTaskTimer(plugin, 20L, batchInterval);
    }

    /**
     * Particle computation runs async: gathers player locations and item data,
     * computes particle positions off the main thread, then queues them for sync flush.
     */
    private void startParticleTask() {
        if (!particlesEnabled) return;
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Gather snapshot data on main thread
                List<ParticleSnapshot> snapshots = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    CachedPlayerItem cached = getCachedItem(player);
                    if (cached == null || cached.item() == null) continue;
                    if (!cached.item().isSphere()) continue;

                    snapshots.add(new ParticleSnapshot(
                        player.getWorld(),
                        player.getLocation().getX(),
                        player.getLocation().getY() + 1.0,
                        player.getLocation().getZ()
                    ));
                }

                if (snapshots.isEmpty()) return;

                // Compute particle positions async and queue them
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    for (ParticleSnapshot snap : snapshots) {
                        computeCircleParticles(snap.world, snap.x, snap.y, snap.z,
                            Particle.DUST, 2, 0.25);
                    }
                });
            }
        };
        particleTask.runTaskTimer(plugin, 10L, particleIntervalTicks);
    }

    private record ParticleSnapshot(World world, double x, double y, double z) {}

    private void computeCircleParticles(World world, double cx, double cy, double cz,
                                         Particle particle, int count, double radius) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = cx + radius * Math.cos(angle);
            double z = cz + radius * Math.sin(angle);
            particleQueue.add(new DeferredParticle(
                world, x, cy, z, particle, 1,
                0, 0, 0, 0,
                particle == Particle.DUST ? new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.0f) : null
            ));
        }
    }

    /**
     * Flushes queued particles on the main thread at a fixed rate.
     * Limits particles per flush to prevent lag spikes.
     */
    private void startParticleFlushTask() {
        particleFlushTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int flushed = 0;
            int maxPerFlush = 100;
            DeferredParticle p;
            while (flushed < maxPerFlush && (p = particleQueue.poll()) != null) {
                if (p.data() != null) {
                    p.world().spawnParticle(p.particle(), p.x(), p.y(), p.z(), p.count(), p.data());
                } else {
                    p.world().spawnParticle(p.particle(), p.x(), p.y(), p.z(), p.count(),
                        p.offsetX(), p.offsetY(), p.offsetZ(), p.extra());
                }
                flushed++;
            }
        }, 2L, 2L);
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

            // Clean expired item cache entries async
            playerItemCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }, 6000L, 6000L).getTaskId();
    }

    // ========== PLAYER-ITEM CACHE ==========

    /**
     * Gets the cached TalismanItem for a player. Returns null if not cached or expired.
     * Cache is populated on passive effect processing and invalidated on item swap/quit.
     */
    private CachedPlayerItem getCachedItem(Player player) {
        CachedPlayerItem cached = playerItemCache.get(player.getUniqueId());
        if (cached != null && !cached.isExpired()) {
            return cached;
        }
        return null;
    }

    /**
     * Resolves and caches the player's offhand TalismanItem.
     * Returns the resolved TalismanItem or null.
     */
    private TalismanItem resolveAndCacheItem(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = getItemId(offhand);

        if (itemId == null) {
            playerItemCache.remove(player.getUniqueId());
            return null;
        }

        Optional<TalismanItem> itemOpt = plugin.getItemManager().getItem(itemId);
        if (itemOpt.isEmpty()) {
            playerItemCache.remove(player.getUniqueId());
            return null;
        }

        TalismanItem item = itemOpt.get();
        playerItemCache.put(player.getUniqueId(),
            new CachedPlayerItem(itemId, item, System.currentTimeMillis()));
        return item;
    }

    /**
     * Gets the player's TalismanItem, using cache if available, otherwise resolving fresh.
     */
    private TalismanItem getPlayerItem(Player player) {
        CachedPlayerItem cached = getCachedItem(player);
        if (cached != null) {
            return cached.item();
        }
        return resolveAndCacheItem(player);
    }

    private String getPlayerItemId(Player player) {
        CachedPlayerItem cached = getCachedItem(player);
        if (cached != null) {
            return cached.itemId();
        }
        resolveAndCacheItem(player);
        CachedPlayerItem fresh = playerItemCache.get(player.getUniqueId());
        return fresh != null ? fresh.itemId() : null;
    }

    // ========== PASSIVE EFFECTS ==========

    private void applyPassiveEffects(Player player) {
        TalismanItem item = resolveAndCacheItem(player);
        String itemId = null;
        CachedPlayerItem cached = playerItemCache.get(player.getUniqueId());
        if (cached != null) {
            itemId = cached.itemId();
        }

        if (item == null) {
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

        lastPassiveItemId.put(player.getUniqueId(), itemId);
        lastPassivePotionEffects.put(player.getUniqueId(), collectPassivePotionEffects(item));

        if (!passiveEffectsEnabled) {
            return;
        }

        applyConfiguredPassivePotions(player, item);
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

    // ========== EVENT HANDLERS ==========

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        TalismanItem item = getPlayerItem(player);
        if (item == null) return;

        mechanicEngine.handleAttackMechanics(player, target, event, item);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        TalismanItem item = getPlayerItem(killer);
        if (item == null) return;

        for (TalismanMechanic mechanic : item.getMechanics()) {
            if (!mechanic.isEnabled() || mechanic.getType() != MechanicType.HEAL_ON_KILL) {
                continue;
            }

            double heal = mechanic.getDouble("heal", 4.0);
            if (heal <= 0) {
                continue;
            }

            killer.setHealth(Math.min(killer.getHealth() + heal, killer.getMaxHealth()));
            mechanicEngine.queueParticle(killer.getLocation(), Particle.HEART);
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

        TalismanItem item = getPlayerItem(player);
        if (item == null) return;

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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        TalismanItem item = getPlayerItem(player);
        if (item == null) return;

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
        // Invalidate cache on item swap
        playerItemCache.remove(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyPassiveEffects(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        playerItemCache.remove(event.getPlayer().getUniqueId());
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
        playerItemCache.remove(uuid);
        mechanicEngine.clearPlayerData(uuid);
    }

    // ========== UTILITIES ==========

    /**
     * Queues a particle for deferred spawning (used by EffectManager internally).
     */
    void queueParticle(Location loc, Particle particle, int count,
                       double offsetX, double offsetY, double offsetZ, double extra) {
        particleQueue.add(new DeferredParticle(
            loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(),
            particle, count, offsetX, offsetY, offsetZ, extra, null
        ));
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
        if (particleFlushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(particleFlushTaskId);
        }
        mechanicEngine.cleanup();
        cooldowns.clear();
        activeEffects.clear();
        lastPassiveItemId.clear();
        lastPassivePotionEffects.clear();
        pandoraPotionExtensionInProgress.clear();
        playerItemCache.clear();
        particleQueue.clear();
    }
}
