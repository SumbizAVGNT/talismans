package me.sumbiz.monntalismans.nexo.mechanics;

import com.nexomc.nexo.mechanics.MechanicFactory;
import me.sumbiz.monntalismans.model.ActivationSlot;
import me.sumbiz.monntalismans.model.TalismanDef;
import me.sumbiz.monntalismans.model.TalismanStackingMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class TalismanMechanicFactory extends MechanicFactory {

    public TalismanMechanicFactory() {
        super("talisman");
    }

    @Override
    public TalismanMechanic parse(ConfigurationSection section) {
        Set<ActivationSlot> slots = parseSlots(section.getStringList("active_in"));

        TalismanStackingMode mode = parseStacking(section.getString("stacking", "NO_STACK"));

        List<TalismanDef.PotionSpec> potions = new ArrayList<>();
        Map<Attribute, Double> attrs = new HashMap<>();

        ConfigurationSection eff = section.getConfigurationSection("effects");
        if (eff != null) {
            for (Map<?, ?> m : eff.getMapList("potions")) {
                PotionEffectType pet = PotionEffectType.getByName(String.valueOf(m.get("type")));
                if (pet == null) continue;

                int amp = intOr(m.get("amplifier"), 0);
                int step = intOr(m.get("amplifier_step"), 1); // НОВОЕ (default 1)
                boolean ambient = boolOr(m.get("ambient"), true);
                boolean particles = boolOr(m.get("particles"), false);
                boolean icon = boolOr(m.get("icon"), true);

                potions.add(new TalismanDef.PotionSpec(pet, amp, step, ambient, particles, icon));
            }

            ConfigurationSection a = eff.getConfigurationSection("attributes");
            if (a != null) {
                for (String k : a.getKeys(false)) {
                    try {
                        Attribute attr = Attribute.valueOf(k.toUpperCase(Locale.ROOT));
                        attrs.put(attr, a.getDouble(k));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        // caps (optional)
        Map<PotionEffectType, Integer> potionCaps = new HashMap<>();
        Map<Attribute, Double> attrCaps = new HashMap<>();

        ConfigurationSection caps = section.getConfigurationSection("caps");
        if (caps != null) {
            ConfigurationSection pc = caps.getConfigurationSection("potions");
            if (pc != null) {
                for (String k : pc.getKeys(false)) {
                    PotionEffectType pet = PotionEffectType.getByName(k);
                    if (pet == null) continue;
                    int cap = pc.getInt(k, Integer.MAX_VALUE);
                    potionCaps.put(pet, cap);
                }
            }

            ConfigurationSection ac = caps.getConfigurationSection("attributes");
            if (ac != null) {
                for (String k : ac.getKeys(false)) {
                    try {
                        Attribute attr = Attribute.valueOf(k.toUpperCase(Locale.ROOT));
                        attrCaps.put(attr, ac.getDouble(k));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        TalismanDef def = new TalismanDef(
                getRootItemId(section),
                slots,
                mode,
                potions,
                attrs,
                potionCaps,
                attrCaps
        );

        TalismanMechanic mech = new TalismanMechanic(this, section, def);
        addToImplemented(mech);
        return mech;
    }

    @Override
    public TalismanMechanic getMechanic(String itemId) {
        return (TalismanMechanic) super.getMechanic(itemId);
    }

    private static TalismanStackingMode parseStacking(String raw) {
        if (raw == null) return TalismanStackingMode.NO_STACK;
        try {
            return TalismanStackingMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return TalismanStackingMode.NO_STACK;
        }
    }

    private static Set<ActivationSlot> parseSlots(List<String> raw) {
        if (raw == null || raw.isEmpty()) return EnumSet.of(ActivationSlot.HOTBAR);

        EnumSet<ActivationSlot> s = EnumSet.noneOf(ActivationSlot.class);
        for (String r : raw) {
            try { s.add(ActivationSlot.valueOf(r.toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ignored) {}
        }
        if (s.isEmpty()) s.add(ActivationSlot.HOTBAR);
        return s;
    }

    private static int intOr(Object o, int def) {
        if (o == null) return def;
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }

    private static boolean boolOr(Object o, boolean def) {
        if (o == null) return def;
        return Boolean.parseBoolean(String.valueOf(o));
    }

    private static String getRootItemId(ConfigurationSection section) {
        ConfigurationSection cur = section;
        ConfigurationSection parent = cur.getParent();
        while (parent != null && parent.getParent() != null) {
            cur = parent;
            parent = cur.getParent();
        }
        return (parent == null) ? cur.getName() : parent.getName();
    }
}
