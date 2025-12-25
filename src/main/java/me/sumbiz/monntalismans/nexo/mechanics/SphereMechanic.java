package me.sumbiz.monntalismans.nexo.mechanics;

import com.nexomc.nexo.items.ItemBuilder;
import com.nexomc.nexo.mechanics.Mechanic;
import me.sumbiz.monntalismans.model.SphereDef;
import me.sumbiz.monntalismans.util.HeadTextureUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.function.Function;

public final class SphereMechanic extends Mechanic {

    private final SphereDef def;

    public SphereMechanic(SphereMechanicFactory factory, ConfigurationSection section, SphereDef def) {
        super(factory, section, createModifier(def));
        this.def = def;
    }

    public SphereDef def() {
        return def;
    }

    /**
     * ВАЖНО:
     * Nexo GUI (/nexo inv) часто показывает referenceCopy() из ItemBuilder,
     * который клонируется из приватного поля "itemStack".
     * Поэтому красим и reference ("itemStack"), и final ("getFinalItemStack()").
     */
    private static Function<ItemBuilder, ItemBuilder> createModifier(SphereDef def) {
        return (builder) -> {
            String b64 = def.headTextureBase64();
            if (b64 == null || b64.isBlank()) return builder;

            // 1) красим reference itemStack (для /nexo inv)
            try {
                Field f = ItemBuilder.class.getDeclaredField("itemStack");
                f.setAccessible(true);
                Object obj = f.get(builder);
                if (obj instanceof ItemStack ref) {
                    HeadTextureUtil.applyBase64IfSkull(ref, b64);
                }
            } catch (Throwable ignored) {}

            // 2) красим final itemStack (для выдачи/билда)
            try {
                HeadTextureUtil.applyBase64IfSkull(builder.getFinalItemStack(), b64);
            } catch (Throwable ignored) {}

            return builder;
        };
    }
}
