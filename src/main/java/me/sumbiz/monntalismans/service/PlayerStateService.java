package me.sumbiz.monntalismans.service;

import com.nexomc.nexo.api.NexoItems;
import me.sumbiz.monntalismans.model.*;
import me.sumbiz.monntalismans.nexo.mechanics.SphereMechanic;
import me.sumbiz.monntalismans.nexo.mechanics.SphereMechanicFactory;
import me.sumbiz.monntalismans.nexo.mechanics.TalismanMechanic;
import me.sumbiz.monntalismans.nexo.mechanics.TalismanMechanicFactory;
import me.sumbiz.monntalismans.util.HeadTextureUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

public final class PlayerStateService implements Listener {

    private final Plugin plugin;

    private final Supplier<SphereMechanicFactory> sphereFactory;
    private final Supplier<TalismanMechanicFactory> talismanFactory;

    private final Supplier<Integer> maxAoeRadius;
    private final Supplier<Integer> maxAoeTargets;
    private final Supplier<Integer> maxPotionsPerUse;
    private final Supplier<Long> maxEffectDurationMs;
    private final Supplier<Long> globalCooldownMs;
    private final Supplier<Set<String>> disabledWorlds;
    private final Supplier<Float> maxExplosionPower;
    private final Supplier<Integer> maxActiveDomesPerWorld;

    private final Supplier<Map<Attribute, Double>> talismanAttributeCapsGlobal;
    private final Supplier<Map<String, Integer>> talismanPotionCapsGlobal;

    private final Set<UUID> queued = new HashSet<>();
    private int potionRefreshTaskId = -1;
    private int domeTaskId = -1;

    private final NamespacedKey kCharges;
    private final NamespacedKey kCooldownUntil;
    private final NamespacedKey kGlobalCooldownUntil;

    private final NamespacedKey kProjSphereId;
    private final NamespacedKey kProjShooter;

    // ✅ НОВОЕ: чтобы не ставить текстуру повторно каждый раз
    private final NamespacedKey kSphereTexHash;

    private final List<ActiveDome> domes = new ArrayList<>();

    private static final long TICK_MS = 50L;

    public PlayerStateService(Plugin plugin,
                              Supplier<SphereMechanicFactory> sphereFactory,
                              Supplier<TalismanMechanicFactory> talismanFactory,
                              Supplier<Integer> maxAoeRadius,
                              Supplier<Integer> maxAoeTargets,
                              Supplier<Integer> maxPotionsPerUse,
                              Supplier<Long> maxEffectDurationMs,
                              Supplier<Long> globalCooldownMs,
                              Supplier<Set<String>> disabledWorlds,
                              Supplier<Float> maxExplosionPower,
                              Supplier<Integer> maxActiveDomesPerWorld,
                              Supplier<Map<Attribute, Double>> talismanAttributeCapsGlobal,
                              Supplier<Map<String, Integer>> talismanPotionCapsGlobal) {
        this.plugin = plugin;

        this.sphereFactory = sphereFactory;
        this.talismanFactory = talismanFactory;

        this.maxAoeRadius = maxAoeRadius;
        this.maxAoeTargets = maxAoeTargets;
        this.maxPotionsPerUse = maxPotionsPerUse;
        this.maxEffectDurationMs = maxEffectDurationMs;
        this.globalCooldownMs = globalCooldownMs;
        this.disabledWorlds = disabledWorlds;
        this.maxExplosionPower = maxExplosionPower;
        this.maxActiveDomesPerWorld = maxActiveDomesPerWorld;

        this.talismanAttributeCapsGlobal = talismanAttributeCapsGlobal;
        this.talismanPotionCapsGlobal = talismanPotionCapsGlobal;

        this.kCharges = new NamespacedKey(plugin, "sphere_charges");
        this.kCooldownUntil = new NamespacedKey(plugin, "sphere_cd_until");
        this.kGlobalCooldownUntil = new NamespacedKey(plugin, "sphere_global_cd_until");

        this.kProjSphereId = new NamespacedKey(plugin, "sphere_proj_id");
        this.kProjShooter = new NamespacedKey(plugin, "sphere_proj_shooter");

        // ✅ НОВОЕ
        this.kSphereTexHash = new NamespacedKey(plugin, "sphere_tex_hash");

        this.potionRefreshTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin, this::refreshPotionsAllOnline, 40L, 40L
        );

        this.domeTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin, this::tickDomes, 10L, 10L
        );
    }

    public void shutdown() {
        if (potionRefreshTaskId != -1) Bukkit.getScheduler().cancelTask(potionRefreshTaskId);
        if (domeTaskId != -1) Bukkit.getScheduler().cancelTask(domeTaskId);
        domes.clear();
    }

    public void rescanAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) scheduleRescan(p);
    }

    private void refreshPotionsAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyTalismans(p, false);
        }
    }

    // ---------------- domes

    private static final class ActiveDome {
        final UUID owner;
        final World world;
        final Location center;
        final int radius;
        final int maxTargets;
        final long endAtMs;
        final int periodTicks;
        final List<SphereDef.PotionTimedSpec> potions;

        long nextApplyMs;

        ActiveDome(UUID owner, Location center, int radius, int maxTargets,
                   long endAtMs, int periodTicks, List<SphereDef.PotionTimedSpec> potions, long now) {
            this.owner = owner;
            this.world = center.getWorld();
            this.center = center;
            this.radius = radius;
            this.maxTargets = maxTargets;
            this.endAtMs = endAtMs;
            this.periodTicks = periodTicks;
            this.potions = potions;
            this.nextApplyMs = now;
        }
    }

    private void tickDomes() {
        long now = System.currentTimeMillis();
        if (domes.isEmpty()) return;

        Iterator<ActiveDome> it = domes.iterator();
        while (it.hasNext()) {
            ActiveDome d = it.next();
            if (d.world == null || now >= d.endAtMs) {
                it.remove();
                continue;
            }
            if (now < d.nextApplyMs) continue;
            d.nextApplyMs = now + (long) d.periodTicks * TICK_MS;

            applyDomeOnce(d);
        }
    }

    private void applyDomeOnce(ActiveDome d) {
        if (d.world == null) return;

        int maxPotions = Math.max(1, maxPotionsPerUse.get());
        long maxDur = Math.max(1000L, maxEffectDurationMs.get());

        double r2 = (double) d.radius * (double) d.radius;

        int affected = 0;
        for (Player target : d.world.getPlayers()) {
            if (affected >= d.maxTargets) break;
            if (target.getLocation().distanceSquared(d.center) > r2) continue;

            int applied = 0;
            for (SphereDef.PotionTimedSpec ps : d.potions) {
                if (applied >= maxPotions) break;

                long durMs = Math.min(maxDur, Math.max(1000L, ps.durationMillis()));
                int ticks = (int) Math.max(20, durMs / 50L);

                target.addPotionEffect(new PotionEffect(ps.type(), ticks, ps.amplifier(), true, false, true));
                applied++;
            }

            affected++;
        }
    }

    private void tryStartDome(Player owner, Location at, SphereDef.DomeSpec dome) {
        if (at == null || at.getWorld() == null) return;

        int worldLimit = Math.max(0, maxActiveDomesPerWorld.get());
        if (worldLimit > 0) {
            int count = 0;
            for (ActiveDome d : domes) {
                if (d.world != null && d.world.equals(at.getWorld())) count++;
            }
            if (count >= worldLimit) {
                owner.sendMessage("§cСлишком много куполов в мире (лимит).");
                return;
            }
        }

        long now = System.currentTimeMillis();

        int radius = Math.min(Math.max(1, dome.radius()), maxAoeRadius.get());
        int targets = dome.maxTargets() > 0 ? dome.maxTargets() : maxAoeTargets.get();
        targets = Math.max(1, targets);

        long duration = Math.min(Math.max(1000L, dome.durationMillis()), maxEffectDurationMs.get());
        int period = Math.max(1, dome.periodTicks());

        Location center = at.clone();
        center.setX(Math.floor(center.getX()) + 0.5);
        center.setY(Math.floor(center.getY()) + 0.5);
        center.setZ(Math.floor(center.getZ()) + 0.5);

        domes.add(new ActiveDome(owner.getUniqueId(), center, radius, targets, now + duration, period, dome.potions(), now));
    }

    // ---------------- events -> rescan

    @EventHandler public void onJoin(PlayerJoinEvent e) { scheduleRescan(e.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { queued.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) { scheduleRescan(e.getPlayer()); }
    @EventHandler public void onHeld(PlayerItemHeldEvent e) { scheduleRescan(e.getPlayer()); }
    @EventHandler public void onSwap(PlayerSwapHandItemsEvent e) { scheduleRescan(e.getPlayer()); }

    @EventHandler(ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) scheduleRescan(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInvDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p) scheduleRescan(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInvClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) scheduleRescan(p);
    }

    // ---------------- spheres

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        Action action = e.getAction();
        boolean rightAir = action == Action.RIGHT_CLICK_AIR;
        boolean rightBlock = action == Action.RIGHT_CLICK_BLOCK;
        if (!rightAir && !rightBlock) return;

        Player p = e.getPlayer();
        if (!p.hasPermission("talismans.use")) return;

        String wn = p.getWorld().getName().toLowerCase(Locale.ROOT);
        if (disabledWorlds.get().contains(wn)) return;

        ItemStack it = p.getInventory().getItemInMainHand();
        String id = NexoItems.idFromItem(it);
        if (id == null) return;

        SphereMechanicFactory sf = sphereFactory.get();
        if (sf == null) return;

        SphereMechanic mech = sf.getMechanic(id);
        if (mech == null) return;

        SphereDef s = mech.def();
        if (!s.activatable()) return;

        if (s.activation() == SphereActivationMode.AIR_ONLY && !rightAir) return;
        if (s.activation() == SphereActivationMode.BLOCK_ONLY && !rightBlock) return;

        if (s.activation() == SphereActivationMode.THROW) {
            e.setCancelled(true);
            tryUseSphereAndConsume(p, it, s, id, true, null);
            return;
        }

        e.setCancelled(true);

        Location at = resolveUseLocation(p, e, s.onUse().at());
        tryUseSphereAndConsume(p, it, s, id, false, at);
    }

    private Location resolveUseLocation(Player p, PlayerInteractEvent e, SphereUseTarget at) {
        if (at == SphereUseTarget.BLOCK && e.getClickedBlock() != null) {
            return e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5);
        }
        return p.getLocation();
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent e) {
        Entity proj = e.getEntity();
        PersistentDataContainer pdc = proj.getPersistentDataContainer();

        String sphereId = pdc.get(kProjSphereId, PersistentDataType.STRING);
        if (sphereId == null) return;

        SphereMechanicFactory sf = sphereFactory.get();
        if (sf == null) return;

        SphereMechanic mech = sf.getMechanic(sphereId);
        if (mech == null) {
            proj.remove();
            return;
        }

        Player owner = null;
        String shooter = pdc.get(kProjShooter, PersistentDataType.STRING);
        if (shooter != null) {
            try { owner = Bukkit.getPlayer(UUID.fromString(shooter)); } catch (IllegalArgumentException ignored) {}
        }

        Location at = proj.getLocation();
        applySphereAt(owner, mech.def(), at, true);

        proj.remove();
    }

    private void tryUseSphereAndConsume(Player p, ItemStack it, SphereDef s, String id, boolean throwMode, Location at) {
        // ✅ даже если в инвентаре ещё Стив — при использовании точно применим base64
        if (s.headTextureBase64() != null) {
            HeadTextureUtil.applyBase64IfSkull(it, s.headTextureBase64());
        }

        long now = System.currentTimeMillis();

        long sphereGcd = Math.max(0, s.globalCooldownMillis());
        long baseGcd = Math.max(0, globalCooldownMs.get());
        long gcd = Math.max(sphereGcd, baseGcd);

        if (gcd > 0) {
            Long gUntil = p.getPersistentDataContainer().get(kGlobalCooldownUntil, PersistentDataType.LONG);
            if (gUntil != null && gUntil > now) {
                long left = gUntil - now;
                p.sendMessage("§cГлобальный кулдаун: " + (left / 1000.0) + "s");
                return;
            }
        }

        var meta = it.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer itemPdc = meta.getPersistentDataContainer();

        Long cdUntil = itemPdc.get(kCooldownUntil, PersistentDataType.LONG);
        if (cdUntil != null && cdUntil > now) {
            long leftMs = cdUntil - now;
            p.sendMessage("§cКулдаун сферы: " + (leftMs / 1000.0) + "s");
            return;
        }

        Integer charges = itemPdc.get(kCharges, PersistentDataType.INTEGER);
        if (charges == null) charges = s.defaultCharges();

        if (charges <= 0) {
            p.sendMessage("§cНет зарядов.");
            return;
        }

        if (throwMode) {
            applySelfPotions(p, s);
            launchSphereProjectile(p, s, id);
        } else {
            applySphereAt(p, s, at, false);
        }

        int newCharges = charges - 1;

        itemPdc.set(kCharges, PersistentDataType.INTEGER, newCharges);
        itemPdc.set(kCooldownUntil, PersistentDataType.LONG, now + Math.max(0, s.cooldownMillis()));
        it.setItemMeta(meta);

        if (gcd > 0) {
            p.getPersistentDataContainer().set(kGlobalCooldownUntil, PersistentDataType.LONG, now + gcd);
        }

        if (newCharges <= 0) {
            int amount = it.getAmount();

            if (amount <= 1) {
                p.getInventory().setItemInMainHand(null);
            } else {
                it.setAmount(amount - 1);

                var m2 = it.getItemMeta();
                if (m2 != null) {
                    PersistentDataContainer p2 = m2.getPersistentDataContainer();
                    p2.set(kCharges, PersistentDataType.INTEGER, s.defaultCharges());
                    p2.remove(kCooldownUntil);
                    it.setItemMeta(m2);
                }
            }

            p.sendMessage("§eСфера использована. Заряды закончились.");
        } else {
            p.sendMessage("§eСфера использована. Осталось зарядов: §f" + newCharges);
        }
    }

    private void applySelfPotions(Player p, SphereDef s) {
        int maxPotions = Math.max(1, maxPotionsPerUse.get());
        long maxDur = Math.max(1000L, maxEffectDurationMs.get());

        int applied = 0;
        for (SphereDef.PotionTimedSpec ps : s.onUse().selfPotions()) {
            if (applied >= maxPotions) break;

            long durMs = Math.min(maxDur, Math.max(1000L, ps.durationMillis()));
            int ticks = (int) Math.max(20, durMs / 50L);

            p.addPotionEffect(new PotionEffect(ps.type(), ticks, ps.amplifier(), true, false, true));
            applied++;
        }
    }

    private void applySphereAt(Player ownerOrNull, SphereDef s, Location at, boolean fromProjectile) {
        if (at == null) return;

        if (!fromProjectile && ownerOrNull != null) {
            applySelfPotions(ownerOrNull, s);
        }

        SphereDef.AoeSpec aoe = s.onUse().aoe();
        if (aoe != null) {
            applyAoePotions(at, aoe);
        }

        if (s.onUse().explosion() != null) {
            doExplosion(ownerOrNull, at, s.onUse().explosion());
        }

        if (s.onUse().dome() != null && ownerOrNull != null) {
            tryStartDome(ownerOrNull, at, s.onUse().dome());
        }
    }

    private void applyAoePotions(Location at, SphereDef.AoeSpec aoe) {
        World w = at.getWorld();
        if (w == null) return;

        int radius = Math.min(Math.max(1, aoe.radius()), maxAoeRadius.get());
        int targets = aoe.maxTargets() > 0 ? aoe.maxTargets() : maxAoeTargets.get();
        targets = Math.max(1, targets);

        int maxPotions = Math.max(1, maxPotionsPerUse.get());
        long maxDur = Math.max(1000L, maxEffectDurationMs.get());

        double r2 = (double) radius * (double) radius;

        int affected = 0;
        for (Player other : w.getPlayers()) {
            if (affected >= targets) break;
            if (other.getLocation().distanceSquared(at) > r2) continue;

            int applied = 0;
            for (SphereDef.PotionTimedSpec ps : aoe.potions()) {
                if (applied >= maxPotions) break;

                long durMs = Math.min(maxDur, Math.max(1000L, ps.durationMillis()));
                int ticks = (int) Math.max(20, durMs / 50L);

                other.addPotionEffect(new PotionEffect(ps.type(), ticks, ps.amplifier(), true, false, true));
                applied++;
            }

            affected++;
        }
    }

    private void doExplosion(Player ownerOrNull, Location at, SphereDef.ExplosionSpec ex) {
        World w = at.getWorld();
        if (w == null) return;

        float power = Math.min(ex.power(), Math.max(0f, maxExplosionPower.get()));
        if (power <= 0f) return;

        boolean setFire = ex.setFire();
        boolean breakBlocks = ex.breakBlocks();

        try {
            Method m = w.getClass().getMethod("createExplosion", Location.class, float.class, boolean.class, boolean.class, Entity.class);
            m.invoke(w, at, power, setFire, breakBlocks, ownerOrNull);
        } catch (Throwable ignored) {
            try { w.createExplosion(at, power, setFire, breakBlocks); } catch (Throwable ignored2) {}
        }
    }

    private void launchSphereProjectile(Player p, SphereDef s, String id) {
        SphereDef.ThrowSpec ts = s.throwSpec();
        double speed = (ts != null) ? ts.speed() : 1.4;
        int lifetime = (ts != null) ? ts.lifetimeTicks() : 60;

        Snowball snowball = p.launchProjectile(Snowball.class);
        snowball.setVelocity(p.getLocation().getDirection().multiply(speed));

        PersistentDataContainer epdc = snowball.getPersistentDataContainer();
        epdc.set(kProjSphereId, PersistentDataType.STRING, id);
        epdc.set(kProjShooter, PersistentDataType.STRING, p.getUniqueId().toString());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (snowball.isValid() && !snowball.isDead()) snowball.remove();
        }, lifetime);
    }

    // ---------------- талисманы

    private static final class Group {
        final TalismanDef def;
        int count;

        Group(TalismanDef def) {
            this.def = def;
            this.count = 1;
        }
    }

    private static final class PotionAgg {
        int amplifier;
        boolean ambient;
        boolean particles;
        boolean icon;

        PotionAgg(int amplifier, boolean ambient, boolean particles, boolean icon) {
            this.amplifier = amplifier;
            this.ambient = ambient;
            this.particles = particles;
            this.icon = icon;
        }
    }

    private void scheduleRescan(Player p) {
        UUID u = p.getUniqueId();
        if (queued.contains(u)) return;
        queued.add(u);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            queued.remove(u);

            // ✅ НОВОЕ: на каждом рескане проставляем текстуру на сферы
            normalizeSpheres(p);

            applyTalismans(p, true);
        }, 1L);
    }

    private void applyTalismans(Player p, boolean alsoAttributes) {
        TalismanMechanicFactory tf = talismanFactory.get();
        if (tf == null) return;

        Map<String, Group> groups = new HashMap<>();

        PlayerInventory inv = p.getInventory();
        ItemStack[] storage = inv.getStorageContents(); // 0..35 (хранилище), хотбар = 0..8

        // HOTBAR
        for (int i = 0; i <= 8 && i < storage.length; i++) {
            addTalisman(groups, storage[i], tf, ActivationSlot.HOTBAR);
        }

        // INVENTORY (без хотбара)
        for (int i = 9; i < storage.length; i++) {
            addTalisman(groups, storage[i], tf, ActivationSlot.INVENTORY);
        }

        // OFFHAND
        addTalisman(groups, inv.getItemInOffHand(), tf, ActivationSlot.OFFHAND);

        // ARMOR
        for (ItemStack it : inv.getArmorContents()) {
            addTalisman(groups, it, tf, ActivationSlot.ARMOR);
        }

        Map<PotionEffectType, PotionAgg> potionsFinal = new HashMap<>();
        Map<Attribute, Double> attrsFinal = new HashMap<>();

        Map<Attribute, Double> globalAttrCaps = talismanAttributeCapsGlobal.get();
        Map<String, Integer> globalPotionCaps = talismanPotionCapsGlobal.get();

        for (Group g : groups.values()) {
            TalismanDef def = g.def;
            int count = g.count;

            for (TalismanDef.PotionSpec ps : def.potions()) {
                int amp = calcPotionAmplifier(def, ps, count, globalPotionCaps);
                PotionEffectType type = ps.type();
                if (type == null) continue;

                PotionAgg cur = potionsFinal.get(type);
                if (cur == null || amp > cur.amplifier) {
                    potionsFinal.put(type, new PotionAgg(amp, ps.ambient(), ps.particles(), ps.icon()));
                }
            }

            for (var e : def.attributes().entrySet()) {
                Attribute attr = e.getKey();
                double v = calcAttributeValue(def, attr, e.getValue(), count);
                attrsFinal.merge(attr, v, Double::sum);
            }
        }

        for (var e : potionsFinal.entrySet()) {
            PotionEffectType type = e.getKey();
            PotionAgg a = e.getValue();
            p.addPotionEffect(new PotionEffect(type, 60, Math.max(0, a.amplifier), a.ambient, a.particles, a.icon));
        }

        if (alsoAttributes) {
            clearOurAttributeModifiers(p);

            if (globalAttrCaps != null && !globalAttrCaps.isEmpty()) {
                for (var cap : globalAttrCaps.entrySet()) {
                    Attribute attr = cap.getKey();
                    Double val = attrsFinal.get(attr);
                    if (val == null) continue;
                    double c = cap.getValue();
                    if (c >= 0) attrsFinal.put(attr, Math.min(val, c));
                    else attrsFinal.put(attr, Math.max(val, c));
                }
            }

            for (var e : attrsFinal.entrySet()) {
                addAttributeModifierSafe(p, e.getKey(), e.getValue());
            }
        }
    }

    private void addTalisman(Map<String, Group> groups, ItemStack it, TalismanMechanicFactory tf, ActivationSlot slot) {
        if (it == null || it.getType().isAir()) return;

        String id = NexoItems.idFromItem(it);
        if (id == null) return;

        TalismanMechanic mech = tf.getMechanic(id);
        if (mech == null) return;

        TalismanDef def = mech.def();
        if (!def.activeIn().contains(slot)) return;

        Group g = groups.get(id);
        if (g == null) groups.put(id, new Group(def));
        else g.count++;
    }

    private int calcPotionAmplifier(TalismanDef def, TalismanDef.PotionSpec ps, int count, Map<String, Integer> globalPotionCaps) {
        int base = ps.amplifier();
        int result;

        switch (def.stacking()) {
            case NO_STACK -> result = base;
            case TAKE_BEST -> result = base;
            case SUM_TO_CAP -> {
                int step = Math.max(0, ps.amplifierStep());
                result = base + Math.max(0, count - 1) * step;

                Integer localCap = def.potionAmpCaps().get(ps.type());
                if (localCap != null) result = Math.min(result, localCap);
            }
            default -> result = base;
        }

        if (globalPotionCaps != null && ps.type() != null) {
            String key = ps.type().getName().toUpperCase(Locale.ROOT);
            Integer cap = globalPotionCaps.get(key);
            if (cap != null) result = Math.min(result, cap);
        }

        return result;
    }

    private double calcAttributeValue(TalismanDef def, Attribute attr, double base, int count) {
        double result;

        switch (def.stacking()) {
            case NO_STACK -> result = base;
            case TAKE_BEST -> result = base;
            case SUM_TO_CAP -> {
                result = base * Math.max(1, count);
                Double cap = def.attributeCaps().get(attr);
                if (cap != null) {
                    if (cap >= 0) result = Math.min(result, cap);
                    else result = Math.max(result, cap);
                }
            }
            default -> result = base;
        }

        return result;
    }

    private void clearOurAttributeModifiers(Player p) {
        for (Attribute a : Attribute.values()) {
            AttributeInstance inst = p.getAttribute(a);
            if (inst == null) continue;

            for (AttributeModifier mod : new ArrayList<>(inst.getModifiers())) {
                if (mod.getName() != null && mod.getName().startsWith("talismans:")) {
                    inst.removeModifier(mod);
                }
            }
        }
    }

    private void addAttributeModifierSafe(Player p, Attribute attr, double amount) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;

        UUID uuid = UUID.nameUUIDFromBytes(("talismans:" + attr.name()).getBytes(StandardCharsets.UTF_8));
        String name = "talismans:" + attr.name();

        try {
            AttributeModifier mod = new AttributeModifier(uuid, name, amount, AttributeModifier.Operation.ADD_NUMBER);
            inst.addModifier(mod);
        } catch (Throwable ignored) {}
    }

    // ✅ НОВОЕ: проставление base64 на сферы в инвентаре (чтобы не был Стив)

    private void normalizeSpheres(Player p) {
        SphereMechanicFactory sf = sphereFactory.get();
        if (sf == null) return;

        PlayerInventory inv = p.getInventory();

        for (ItemStack it : inv.getStorageContents()) normalizeSphereItem(it, sf);
        normalizeSphereItem(inv.getItemInOffHand(), sf);
        for (ItemStack it : inv.getArmorContents()) normalizeSphereItem(it, sf);
    }

    private void normalizeSphereItem(ItemStack it, SphereMechanicFactory sf) {
        if (it == null || it.getType().isAir()) return;

        String id = NexoItems.idFromItem(it);
        if (id == null) return;

        SphereMechanic mech = sf.getMechanic(id);
        if (mech == null) return;

        SphereDef def = mech.def();
        String b64 = def.headTextureBase64();
        if (b64 == null || b64.isBlank()) return;

        var meta = it.getItemMeta();
        if (!(meta instanceof SkullMeta skull)) return;

        PersistentDataContainer pdc = skull.getPersistentDataContainer();
        int hash = b64.hashCode();

        Integer prev = pdc.get(kSphereTexHash, PersistentDataType.INTEGER);
        if (prev != null && prev == hash) return;

        if (HeadTextureUtil.applyBase64ToSkullMeta(skull, b64)) {
            pdc.set(kSphereTexHash, PersistentDataType.INTEGER, hash);
            it.setItemMeta(skull);
        }
    }
}
