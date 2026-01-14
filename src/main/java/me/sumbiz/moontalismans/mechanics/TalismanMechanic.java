package me.sumbiz.moontalismans.mechanics;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Конфигурация отдельной механики талисмана/сферы.
 * Содержит все параметры для работы механики.
 */
public class TalismanMechanic {
    private final MechanicType type;
    private final boolean enabled;
    private final Map<String, Object> parameters;

    public TalismanMechanic(MechanicType type, boolean enabled, Map<String, Object> parameters) {
        this.type = type;
        this.enabled = enabled;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    public MechanicType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Методы получения параметров с дефолтными значениями

    public double getDouble(String key, double defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof String str) {
            return str;
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return defaultValue;
    }

    public PotionEffectConfig getPotionEffect(String key) {
        Object value = parameters.get(key);
        if (value instanceof Map<?, ?> map) {
            return PotionEffectConfig.fromMap(map);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<PotionEffectConfig> getPotionEffectList(String key) {
        Object value = parameters.get(key);
        if (value instanceof List<?> list) {
            List<PotionEffectConfig> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    PotionEffectConfig effect = PotionEffectConfig.fromMap(map);
                    if (effect != null) {
                        result.add(effect);
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Парсинг механики из конфига.
     * Формат:
     * mechanic_name:
     *   type: REVIVE_ON_DEATH
     *   enabled: true
     *   cooldown: 300000
     *   health_multiplier: 0.3
     *   effects:
     *     - type: REGENERATION
     *       duration: 100
     *       amplifier: 2
     */
    public static TalismanMechanic fromConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String typeStr = section.getString("type");
        if (typeStr == null) {
            return null;
        }

        MechanicType type;
        try {
            type = MechanicType.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }

        boolean enabled = section.getBoolean("enabled", true);

        // Собираем все параметры из секции
        Map<String, Object> parameters = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (!key.equals("type") && !key.equals("enabled")) {
                parameters.put(key, section.get(key));
            }
        }

        return new TalismanMechanic(type, enabled, parameters);
    }

    /**
     * Конфигурация эффекта зелья для механики.
     */
    public record PotionEffectConfig(PotionEffectType type, int duration, int amplifier, boolean ambient, boolean particles,
                                     boolean icon) {
        public static PotionEffectConfig fromMap(Map<?, ?> map) {
            Object typeObj = map.get("type");
            if (!(typeObj instanceof String typeStr)) {
                return null;
            }

            PotionEffectType type = PotionEffectType.getByName(typeStr.toUpperCase(Locale.ROOT));
            if (type == null) {
                return null;
            }

            Object durationObj = map.containsKey("duration") ? map.get("duration") : 60;
            Object amplifierObj = map.containsKey("amplifier") ? map.get("amplifier") : 0;
            Object ambientObj = map.containsKey("ambient") ? map.get("ambient") : true;
            Object particlesObj = map.containsKey("particles") ? map.get("particles") : false;
            Object iconObj = map.containsKey("icon") ? map.get("icon") : true;

            int duration = durationObj instanceof Number number ? number.intValue() : 60;
            int amplifier = amplifierObj instanceof Number number ? number.intValue() : 0;
            boolean ambient = ambientObj instanceof Boolean value ? value : true;
            boolean particles = particlesObj instanceof Boolean value ? value : false;
            boolean icon = iconObj instanceof Boolean value ? value : true;

            return new PotionEffectConfig(type, duration, amplifier, ambient, particles, icon);
        }
    }

    @Override
    public String toString() {
        return "TalismanMechanic{" +
                "type=" + type +
                ", enabled=" + enabled +
                ", params=" + parameters.size() +
                '}';
    }
}
