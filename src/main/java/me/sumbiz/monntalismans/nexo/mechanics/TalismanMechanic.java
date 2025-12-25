package me.sumbiz.monntalismans.nexo.mechanics;

import com.nexomc.nexo.mechanics.Mechanic;
import me.sumbiz.monntalismans.model.TalismanDef;
import org.bukkit.configuration.ConfigurationSection;

import java.util.function.Function;

public final class TalismanMechanic extends Mechanic {

    private final TalismanDef def;

    @SuppressWarnings({"rawtypes","unchecked"})
    public TalismanMechanic(TalismanMechanicFactory factory, ConfigurationSection section, TalismanDef def) {
        super(factory, section, new Function[0]);
        this.def = def;
    }

    public TalismanDef def() {
        return def;
    }
}
