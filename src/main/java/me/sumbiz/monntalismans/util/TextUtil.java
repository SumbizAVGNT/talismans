package me.sumbiz.monntalismans.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public final class TextUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {}

    public static Component parse(String s) {
        if (s == null) return Component.empty();

        // если похоже на minimessage
        if (s.contains("<") && s.contains(">")) {
            try { return MM.deserialize(s); } catch (Exception ignored) {}
        }

        return LEGACY.deserialize(s);
    }

    public static List<Component> parseList(List<String> lines) {
        List<Component> out = new ArrayList<>();
        if (lines == null) return out;
        for (String l : lines) out.add(parse(l));
        return out;
    }
}
