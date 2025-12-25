package me.sumbiz.monntalismans.service;

import me.sumbiz.monntalismans.model.ItemDef;
import me.sumbiz.monntalismans.model.ItemType;
import me.sumbiz.monntalismans.model.anarchy.AnarchyMechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Лёгкая служба с уникальными механиками для анархичных серверов.
 */
public final class AnarchyMechanicService implements Listener {

    private final Plugin plugin;
    private final ItemService items;
    private Map<String, ItemDef> defs;

    private final Cooldowns cooldowns = new Cooldowns();

    public AnarchyMechanicService(Plugin plugin, ItemService items, Map<String, ItemDef> defs) {
        this.plugin = plugin;
        this.items = items;
        this.defs = defs;
    }

    public void reload(Map<String, ItemDef> defs) {
        this.defs = defs;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Optional<Set<AnarchyMechanic>> mechanics = mechanicsFor(player);
        if (mechanics.isEmpty()) return;

        if (mechanics.get().contains(AnarchyMechanic.CHAOS_SHIELD)) {
            long until = cooldowns.until(player.getUniqueId(), AnarchyMechanic.CHAOS_SHIELD, 4000);
            if (until == -1L) {
                event.setDamage(event.getDamage() * 0.65);
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 6, 1, true, false, true));
                cooldowns.put(player.getUniqueId(), AnarchyMechanic.CHAOS_SHIELD, 12000);
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 20, 0.4, 0.8, 0.4);
            }
        }

        if (mechanics.get().contains(AnarchyMechanic.RELIC_GUARD) && event.getFinalDamage() >= player.getHealth()) {
            ItemStack relic = findRelic(player);
            if (relic != null) {
                event.setCancelled(true);
                relic.setAmount(relic.getAmount() - 1);
                player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 12));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 8, 1, true, true, true));
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 0.8f);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        Optional<Set<AnarchyMechanic>> mechanics = mechanicsFor(player);
        if (mechanics.isEmpty()) return;

        if (mechanics.get().contains(AnarchyMechanic.BLOOD_LINK)) {
            double heal = Math.max(1.0, event.getFinalDamage() * 0.18);
            player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + heal));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 4, 0.2, 0.2, 0.2);
        }

        if (mechanics.get().contains(AnarchyMechanic.METEOR_CALL) && ThreadLocalRandom.current().nextDouble() < 0.15) {
            Location spawn = event.getEntity().getLocation().add(0, 8, 0);
            Fireball fireball = event.getEntity().getWorld().spawn(spawn, Fireball.class, f -> {
                f.setDirection(event.getEntity().getLocation().toVector().subtract(spawn.toVector()).normalize());
                f.setYield(2.0f);
                f.setIsIncendiary(false);
                f.setPersistent(false);
            });
            fireball.setVelocity(fireball.getDirection().multiply(0.4));
            event.getEntity().getWorld().playSound(spawn, Sound.ENTITY_GHAST_SHOOT, 1f, 0.6f);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) return;
        Optional<Set<AnarchyMechanic>> mechanics = mechanicsFor(player);
        if (mechanics.isEmpty()) return;

        if (mechanics.get().contains(AnarchyMechanic.VOID_STEP)) {
            long until = cooldowns.until(player.getUniqueId(), AnarchyMechanic.VOID_STEP, 3000);
            if (until != -1L) return;

            Location from = player.getLocation();
            Location target = from.clone().add(from.getDirection().normalize().multiply(6));
            target.setY(Math.max(from.getWorld().getMinHeight(), Math.min(target.getY(), from.getWorld().getMaxHeight())));
            player.teleport(target);
            player.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.25f);
            player.spawnParticle(Particle.PORTAL, target, 40, 0.5, 1.0, 0.5, 0.1);
            cooldowns.put(player.getUniqueId(), AnarchyMechanic.VOID_STEP, 8000);
        }
    }

    @EventHandler
    public void onItemSwap(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Optional<Set<AnarchyMechanic>> mechanics = mechanicsFor(player);
        if (mechanics.isEmpty()) return;

        if (mechanics.get().contains(AnarchyMechanic.OVERCHARGE)) {
            long until = cooldowns.until(player.getUniqueId(), AnarchyMechanic.OVERCHARGE, 2000);
            if (until == -1L) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 1, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 5, 0, true, false, true));
                cooldowns.put(player.getUniqueId(), AnarchyMechanic.OVERCHARGE, 15000);
            }
        }
    }

    private ItemStack findRelic(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) continue;
            if (stack.getType() == Material.NETHER_STAR || stack.getType() == Material.TOTEM_OF_UNDYING) {
                return stack;
            }
        }
        return null;
    }

    private Optional<Set<AnarchyMechanic>> mechanicsFor(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        String id = items.getItemId(main);
        ItemType type = items.getItemType(main);
        if (id == null) {
            id = items.getItemId(off);
            type = items.getItemType(off);
        }
        if (id == null) return Optional.empty();

        ItemDef def = defs.get(id);
        if (def == null) return Optional.empty();

        if (type == ItemType.SPHERE && def instanceof ItemDef.Sphere sphere) {
            return Optional.ofNullable(sphere.def().anarchyMechanics());
        }
        if (type == ItemType.TALISMAN && def instanceof ItemDef.Talisman talisman) {
            return Optional.ofNullable(talisman.def().anarchyMechanics());
        }
        return Optional.empty();
    }

    private static final class Cooldowns {
        private final Map<String, Long> cd = new java.util.concurrent.ConcurrentHashMap<>();

        long until(UUID uuid, AnarchyMechanic mechanic, long jitterMs) {
            long now = System.currentTimeMillis();
            long key = cd.getOrDefault(uuid.toString() + mechanic.name(), -1L);
            if (key == -1L) return -1L;
            if (key <= now) return -1L;
            return key - now;
        }

        void put(UUID uuid, AnarchyMechanic mechanic, long millis) {
            cd.put(uuid.toString() + mechanic.name(), System.currentTimeMillis() + millis);
        }
    }
}
