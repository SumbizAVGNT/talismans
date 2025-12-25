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

        startPassiveEffectTask();
        startParticleTask();
    }

    /**
     * Task that applies passive potion effects every 2 seconds.
     */
    private void startPassiveEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyPassiveEffects(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 40L); // Every 2 seconds
    }

    /**
     * Task that spawns particles around players with special items.
     */
    private void startParticleTask() {
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    spawnAmbientParticles(player);
                }
            }
        };
        particleTask.runTaskTimer(plugin, 10L, 10L); // Every 0.5 seconds
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
    }

    /**
     * Apply passive effects based on talisman/sphere type.
     */
    private void applyBuiltInPassiveEffects(Player player, TalismanItem item) {
        String id = item.getId().toLowerCase();

        applyConfiguredPassivePotions(player, item);

        // Феникс - регенерация при низком здоровье
        if (id.contains("feniksa") || id.contains("phoenix")) {
            if (player.getHealth() < player.getMaxHealth() * 0.3) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, true, false, true));
            }
        }

        // Лекарь - постоянная медленная регенерация
        if (id.contains("lekarya") || id.contains("healer")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false, true));
        }

        // Тритон - водное дыхание
        if (id.contains("tritona") || id.contains("triton")) {
            if (player.isInWater()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 100, 0, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 100, 0, true, false, true));
            }
        }

        // Грани - ускорение в бою
        if (id.contains("grani") || id.contains("grani")) {
            if (player.getLastDamage() > 0 && (System.currentTimeMillis() - player.getLastDamageCause().getEntity().getTicksLived()) < 100) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, true));
            }
        }

        // Эгида - сопротивление урону
        if (id.contains("egida") || id.contains("aegis")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, true));
        }

        // Магма - огнестойкость
        if (id.contains("magma")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false, true));
        }

        // Афина - боевая ярость
        if (id.contains("afina") || id.contains("athena")) {
            // Gives strength when health drops below 50%
            if (player.getHealth() < player.getMaxHealth() * 0.5) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, true, false, true));
            }
        }

        // Теургия - дополнительное поглощение
        if (id.contains("teurgia") || id.contains("theurgy")) {
            if (!player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 0, true, false, true));
            }
        }

        // Иасо - усиленное исцеление
        if (id.contains("iaso")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 0, true, false, true));
        }
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
        if (id.contains("kobry") || id.contains("cobra")) {
            if (Math.random() < 0.3) { // 30% chance
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, true, true, true));
                spawnHitParticles(target.getLocation(), Particle.WITCH);
            }
        }

        // Ехидна - высасывание жизни
        if (id.contains("ekhidny") || id.contains("echidna")) {
            double heal = event.getDamage() * 0.15; // 15% lifesteal
            player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
            spawnHitParticles(player.getLocation(), Particle.HEART);
        }

        // Каратель - дополнительный урон при критическом ударе
        if (id.contains("karatelya") || id.contains("punisher")) {
            if (player.getFallDistance() > 0 && !player.isOnGround()) {
                event.setDamage(event.getDamage() * 1.2); // 20% extra crit damage
                spawnHitParticles(target.getLocation(), Particle.ENCHANTED_HIT);
            }
        }

        // Крайт - замедление при ударе
        if (id.contains("kraita") || id.contains("krait")) {
            if (Math.random() < 0.25) { // 25% chance
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, true, true, true));
                spawnHitParticles(target.getLocation(), Particle.SNOWFLAKE);
            }
        }

        // Крушитель - оглушение при ударе
        if (id.contains("krushitelya") || id.contains("crusher")) {
            if (Math.random() < 0.2) { // 20% chance
                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 2, true, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, true, true, true));
                spawnHitParticles(target.getLocation(), Particle.SONIC_BOOM);
            }
        }

        // Химера - случайные эффекты
        if (id.contains("chimera") || id.contains("himera")) {
            if (Math.random() < 0.35) { // 35% chance
                applyRandomChimeraEffect(target);
            }
        }

        // Пандора - распространение эффектов
        if (id.contains("pandora")) {
            if (Math.random() < 0.25) { // 25% chance
                for (Entity nearby : target.getNearbyEntities(3, 3, 3)) {
                    if (nearby instanceof LivingEntity le && nearby != player) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, true, true));
                    }
                }
                spawnHitParticles(target.getLocation(), Particle.SCULK_CHARGE_POP);
            }
        }
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
        target.addPotionEffect(new PotionEffect(effect, 60, 0, true, true, true));

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
        if (id.contains("egida") || id.contains("aegis")) {
            event.setDamage(event.getDamage() * 0.9); // 10% damage reduction
        }

        // Грани - ускорение при получении урона
        if (id.contains("grani")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, true));
        }

        // Титан - устойчивость к отбрасыванию (дополнительная)
        if (id.contains("titan")) {
            // The knockback resistance is already in attributes, but we add extra effect
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, true, false, true));
        }

        // Феникс - второй шанс
        if (id.contains("feniksa") || id.contains("phoenix")) {
            if (player.getHealth() - event.getFinalDamage() <= 0) {
                if (!isOnCooldown(player, "phoenix_revive")) {
                    event.setCancelled(true);
                    player.setHealth(player.getMaxHealth() * 0.3);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, true, false, true));

                    // Spawn revival effect
                    Location loc = player.getLocation();
                    player.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                    player.getWorld().spawnParticle(Particle.LAVA, loc, 20, 0.5, 0.5, 0.5, 0);
                    player.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);

                    setCooldown(player, "phoenix_revive", 300000); // 5 minute cooldown
                    player.sendMessage(Component.text("⚔ ").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
                            .append(Component.text("Талисман Феникса спас вас от смерти! ").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                            .append(Component.text("(Кулдаун: 5 минут)").color(NamedTextColor.GRAY)));
                }
            }
        }

        item.getDamageReflect().ifPresent(reflectMultiplier -> {
            if (event instanceof EntityDamageByEntityEvent ede && ede.getDamager() instanceof LivingEntity attacker) {
                double reflect = event.getDamage() * reflectMultiplier;
                attacker.damage(reflect, player);
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            }
        });
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
        return pdc.get(keyId, PersistentDataType.STRING);
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
        cooldowns.clear();
        activeEffects.clear();
    }
}
