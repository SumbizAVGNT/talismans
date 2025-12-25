package me.sumbiz.monntalismans.model;

import org.bukkit.Material;

public record TemplateDef(
        String fromNexoId,   // может быть null
        Material material    // fallback, если Nexo нет
) {}
