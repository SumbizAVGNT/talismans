package me.sumbiz.monntalismans.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class HeadTextureUtil {

    private HeadTextureUtil() {}

    public static boolean applyBase64IfSkull(ItemStack item, String textureBase64) {
        if (item == null) return false;
        if (textureBase64 == null || textureBase64.isBlank()) return false;

        if (!(item.getItemMeta() instanceof SkullMeta meta)) return false;

        boolean ok = applyBase64ToSkullMeta(meta, textureBase64);
        if (ok) item.setItemMeta(meta);
        return ok;
    }

    public static boolean applyBase64ToSkullMeta(SkullMeta skullMeta, String textureBase64) {
        if (skullMeta == null) return false;
        if (textureBase64 == null || textureBase64.isBlank()) return false;

        String skinUrl = skinUrlFromBase64(textureBase64);
        if (skinUrl == null || skinUrl.isBlank()) return false;

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "sphere");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(skinUrl));
            profile.setTextures(textures);

            skullMeta.setOwnerProfile(profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String skinUrlFromBase64(String raw) {
        try {
            String normalized = normalizeBase64(raw);
            String decoded = new String(Base64.getDecoder().decode(normalized), StandardCharsets.UTF_8);

            int http = decoded.indexOf("http");
            if (http < 0) return null;

            int end = decoded.indexOf('"', http);
            if (end < 0) end = decoded.length();

            return decoded.substring(http, end);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String normalizeBase64(String s) {
        String out = s.trim();

        if (out.length() >= 2 && out.startsWith("\"") && out.endsWith("\"")) out = out.substring(1, out.length() - 1);
        if (out.length() >= 2 && out.startsWith("'") && out.endsWith("'")) out = out.substring(1, out.length() - 1);

        out = out.replace("\n", "").replace("\r", "").replace(" ", "");

        int pad = (4 - (out.length() % 4)) % 4;
        if (pad != 0) out = out + "=".repeat(pad);

        return out;
    }
}
