package me.sumbiz.monntalismans.nexo.mechanics;

import com.nexomc.nexo.mechanics.MechanicFactory;
import me.sumbiz.monntalismans.model.SphereActivationMode;
import me.sumbiz.monntalismans.model.SphereDef;
import me.sumbiz.monntalismans.model.SphereUseTarget;
import me.sumbiz.monntalismans.util.DurationParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SphereMechanicFactory extends MechanicFactory {

    public SphereMechanicFactory() {
        super("sphere");
    }

    @Override
    public SphereMechanic parse(ConfigurationSection section) {
        boolean activatable = section.getBoolean("activatable", true);

        SphereActivationMode activation = parseActivation(section.getString("activation", "RIGHT_CLICK"));
        long cooldown = DurationParser.parseToMillis(section.getString("cooldown", "30s"));
        long globalCooldown = DurationParser.parseToMillis(section.getString("global_cooldown", "0s"));

        int charges = Math.max(1, section.getInt("charges", 1));
        String base64 = section.getString("head_texture_base64", null);

        SphereDef.ThrowSpec throwSpec = null;
        if (activation == SphereActivationMode.THROW) {
            ConfigurationSection t = section.getConfigurationSection("throw");
            double speed = (t != null) ? t.getDouble("speed", 1.4) : 1.4;
            int lifetime = (t != null) ? t.getInt("lifetime_ticks", 60) : 60;
            throwSpec = new SphereDef.ThrowSpec(speed, Math.max(5, lifetime));
        }

        SphereDef.UseSpec use = parseUse(section.getConfigurationSection("on_use"), activation);

        SphereDef def = new SphereDef(
                getRootItemId(section),
                activatable,
                activation,
                cooldown,
                globalCooldown,
                charges,
                base64,
                throwSpec,
                use
        );

        SphereMechanic mech = new SphereMechanic(this, section, def);
        addToImplemented(mech);
        return mech;
    }

    @Override
    public SphereMechanic getMechanic(String itemId) {
        return (SphereMechanic) super.getMechanic(itemId);
    }

    private static SphereActivationMode parseActivation(String raw) {
        if (raw == null) return SphereActivationMode.RIGHT_CLICK;
        try {
            return SphereActivationMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SphereActivationMode.RIGHT_CLICK;
        }
    }

    private static SphereUseTarget defaultTargetFor(SphereActivationMode act) {
        return switch (act) {
            case BLOCK_ONLY -> SphereUseTarget.BLOCK;
            case THROW -> SphereUseTarget.HIT;
            default -> SphereUseTarget.PLAYER;
        };
    }

    private static SphereUseTarget parseTarget(String raw, SphereActivationMode act) {
        if (raw == null || raw.isBlank()) return defaultTargetFor(act);
        try {
            return SphereUseTarget.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return defaultTargetFor(act);
        }
    }

    private static SphereDef.UseSpec parseUse(ConfigurationSection s, SphereActivationMode act) {
        SphereUseTarget at = defaultTargetFor(act);
        if (s == null) return SphereDef.UseSpec.empty(at);

        at = parseTarget(s.getString("at", null), act);

        // self_potions
        List<SphereDef.PotionTimedSpec> self = new ArrayList<>();
        for (Map<?, ?> m : s.getMapList("self_potions")) {
            PotionEffectType pet = PotionEffectType.getByName(String.valueOf(m.get("type")));
            if (pet == null) continue;

            int amp = intOr(m.get("amplifier"), 0);

            Object durObj = m.get("duration");
            String durStr = (durObj == null) ? "5s" : String.valueOf(durObj);
            long dur = DurationParser.parseToMillis(durStr);

            self.add(new SphereDef.PotionTimedSpec(pet, amp, dur));
        }

        // aoe
        SphereDef.AoeSpec aoe = null;
        ConfigurationSection a = s.getConfigurationSection("aoe");
        if (a != null) {
            int radius = Math.max(1, a.getInt("radius", 6));
            int maxTargets = Math.max(0, a.getInt("max_targets", 0));

            List<SphereDef.PotionTimedSpec> ps = new ArrayList<>();
            for (Map<?, ?> m : a.getMapList("potions")) {
                PotionEffectType pet = PotionEffectType.getByName(String.valueOf(m.get("type")));
                if (pet == null) continue;

                int amp = intOr(m.get("amplifier"), 0);

                Object durObj = m.get("duration");
                String durStr = (durObj == null) ? "5s" : String.valueOf(durObj);
                long dur = DurationParser.parseToMillis(durStr);

                ps.add(new SphereDef.PotionTimedSpec(pet, amp, dur));
            }

            aoe = new SphereDef.AoeSpec(radius, maxTargets, ps);
        }

        // explosion
        SphereDef.ExplosionSpec explosion = null;
        ConfigurationSection ex = s.getConfigurationSection("explosion");
        if (ex != null) {
            float power = (float) ex.getDouble("power", 0.0);
            if (power > 0.0f) {
                boolean setFire = ex.getBoolean("set_fire", false);
                boolean breakBlocks = ex.getBoolean("break_blocks", false);
                explosion = new SphereDef.ExplosionSpec(power, setFire, breakBlocks);
            }
        }

        // dome
        SphereDef.DomeSpec dome = null;
        ConfigurationSection d = s.getConfigurationSection("dome");
        if (d != null) {
            int radius = Math.max(1, d.getInt("radius", 6));
            int maxTargets = Math.max(0, d.getInt("max_targets", 0));
            long duration = DurationParser.parseToMillis(d.getString("duration", "8s"));
            int periodTicks = Math.max(1, d.getInt("period_ticks", 10));

            List<SphereDef.PotionTimedSpec> ps = new ArrayList<>();
            for (Map<?, ?> m : d.getMapList("potions")) {
                PotionEffectType pet = PotionEffectType.getByName(String.valueOf(m.get("type")));
                if (pet == null) continue;

                int amp = intOr(m.get("amplifier"), 0);

                Object durObj = m.get("duration");
                String durStr = (durObj == null) ? "2s" : String.valueOf(durObj);
                long dur = DurationParser.parseToMillis(durStr);

                ps.add(new SphereDef.PotionTimedSpec(pet, amp, dur));
            }

            dome = new SphereDef.DomeSpec(radius, maxTargets, duration, periodTicks, ps);
        }

        return new SphereDef.UseSpec(at, self, aoe, explosion, dome);
    }

    private static int intOr(Object o, int def) {
        if (o == null) return def;
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
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
