package me.sumbiz.monntalismans.model;

import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * Слоты для активации атрибутов талисманов и сфер.
 */
public enum ActivationSlot {
    MAINHAND(EquipmentSlotGroup.MAINHAND),
    OFFHAND(EquipmentSlotGroup.OFFHAND),
    HAND(EquipmentSlotGroup.HAND),
    HEAD(EquipmentSlotGroup.HEAD),
    CHEST(EquipmentSlotGroup.CHEST),
    LEGS(EquipmentSlotGroup.LEGS),
    FEET(EquipmentSlotGroup.FEET),
    ARMOR(EquipmentSlotGroup.ARMOR),
    ANY(EquipmentSlotGroup.ANY);

    private final EquipmentSlotGroup bukkitSlot;

    ActivationSlot(EquipmentSlotGroup bukkitSlot) {
        this.bukkitSlot = bukkitSlot;
    }

    public EquipmentSlotGroup toBukkit() {
        return bukkitSlot;
    }

    public static ActivationSlot fromString(String s) {
        if (s == null) return OFFHAND;
        return switch (s.toLowerCase()) {
            case "mainhand", "main_hand" -> MAINHAND;
            case "offhand", "off_hand" -> OFFHAND;
            case "hand" -> HAND;
            case "head", "helmet" -> HEAD;
            case "chest", "chestplate" -> CHEST;
            case "legs", "leggings" -> LEGS;
            case "feet", "boots" -> FEET;
            case "armor" -> ARMOR;
            case "any" -> ANY;
            default -> OFFHAND;
        };
    }
}
