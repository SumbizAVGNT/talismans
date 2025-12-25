package me.sumbiz.monntalismans.service;

import me.sumbiz.monntalismans.model.*;
import me.sumbiz.monntalismans.util.HeadTextureUtil;
import me.sumbiz.monntalismans.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ItemService {

    private final Plugin plugin;
    private final NexoBridge nexo;

    private Map<String, ItemDef> items;

    private final NamespacedKey kId;
    private final NamespacedKey kType;

    public ItemService(Plugin plugin, Map<String, ItemDef> items, NexoBridge nexo) {
        this.plugin = plugin;
        this.items = items;
        this.nexo = nexo;

        this.kId = new NamespacedKey(plugin, "mt_id");
        this.kType = new NamespacedKey(plugin, "mt_type");
    }

    public void reload(Map<String, ItemDef> items) {
        this.items = items;
    }

    public Set<String> ids() {
        return items.keySet();
    }

    public ItemDef getDefinition(String id) {
        return items.get(id);
    }

    public ItemStack create(String id, int amount) {
        ItemDef def = items.get(id);
        if (def == null) return null;

        ItemStack item = buildBaseItem(def);
        if (item == null) return null;

        applyMeta(item, def);
        item.setAmount(Math.max(1, amount));
        return item;
    }

    private ItemStack buildBaseItem(ItemDef itemDef) {
        if (itemDef instanceof ItemDef.Sphere s) {
            SphereDef.ResourceDef res = s.def().resource();
            return buildFromResource(res.material(), res.fromNexoId());
        } else if (itemDef instanceof ItemDef.Talisman t) {
            TalismanDef.ResourceDef res = t.def().resource();
            return buildFromResource(res.material(), res.fromNexoId());
        }
        return null;
    }

    private ItemStack buildFromResource(Material material, String fromNexoId) {
        // Сначала пробуем Nexo
        if (fromNexoId != null && !fromNexoId.isBlank() && nexo.isAvailable()) {
            ItemStack nexoItem = nexo.buildItem(fromNexoId);
            if (nexoItem != null) return nexoItem;
        }
        return new ItemStack(material);
    }

    private void applyMeta(ItemStack item, ItemDef def) {
        if (def instanceof ItemDef.Sphere s) {
            applySphere(item, s.def());
        } else if (def instanceof ItemDef.Talisman t) {
            applyTalisman(item, t.def());
        }
    }

    private void applySphere(ItemStack item, SphereDef sd) {
        // Сначала применяем текстуру головы (если есть)
        if (sd.headTextureBase64() != null && !sd.headTextureBase64().isBlank()) {
            HeadTextureUtil.applyBase64IfSkull(item, sd.headTextureBase64());
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Имя и описание
        meta.displayName(TextUtil.parse(sd.displayName()));
        meta.lore(TextUtil.parseList(sd.lore()));

        // Свечение
        meta.setEnchantmentGlintOverride(sd.glint());

        // Флаги предмета
        for (ItemFlag flag : sd.itemFlags()) {
            meta.addItemFlags(flag);
        }

        // Атрибуты
        applyAttributes(meta, sd.attributeModifiers(), sd.id());

        // PDC
        meta.getPersistentDataContainer().set(kId, PersistentDataType.STRING, sd.id());
        meta.getPersistentDataContainer().set(kType, PersistentDataType.STRING, ItemType.SPHERE.name());

        item.setItemMeta(meta);
    }

    private void applyTalisman(ItemStack item, TalismanDef td) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Имя и описание
        meta.displayName(TextUtil.parse(td.displayName()));
        meta.lore(TextUtil.parseList(td.lore()));

        // Свечение
        meta.setEnchantmentGlintOverride(td.glint());

        // Флаги предмета
        for (ItemFlag flag : td.itemFlags()) {
            meta.addItemFlags(flag);
        }

        // Атрибуты
        applyAttributes(meta, td.attributeModifiers(), td.id());

        // PDC
        meta.getPersistentDataContainer().set(kId, PersistentDataType.STRING, td.id());
        meta.getPersistentDataContainer().set(kType, PersistentDataType.STRING, ItemType.TALISMAN.name());

        item.setItemMeta(meta);
    }

    private void applyAttributes(ItemMeta meta, Map<ActivationSlot, Map<Attribute, Double>> modifiers, String itemId) {
        if (modifiers == null || modifiers.isEmpty()) return;

        for (Map.Entry<ActivationSlot, Map<Attribute, Double>> slotEntry : modifiers.entrySet()) {
            ActivationSlot slot = slotEntry.getKey();
            Map<Attribute, Double> attrs = slotEntry.getValue();

            for (Map.Entry<Attribute, Double> attrEntry : attrs.entrySet()) {
                Attribute attr = attrEntry.getKey();
                double value = attrEntry.getValue();

                AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;

                NamespacedKey key = new NamespacedKey(plugin, itemId + "_" + attr.name().toLowerCase() + "_" + slot.name().toLowerCase());

                UUID uuid = UUID.nameUUIDFromBytes((key.toString() + slot.name()).getBytes(StandardCharsets.UTF_8));
                AttributeModifier modifier = new AttributeModifier(uuid, key.toString(), value, operation, slot.toBukkit());

                meta.addAttributeModifier(attr, modifier);
            }
        }
    }

    public String debugItemInHand(Player p) {
        ItemStack it = p.getInventory().getItemInMainHand();
        if (it == null || it.getType().isAir()) return "§eВ руке пусто.";

        ItemMeta meta = it.getItemMeta();
        if (meta == null) return "§eНет меты.";

        String id = meta.getPersistentDataContainer().get(kId, PersistentDataType.STRING);
        String type = meta.getPersistentDataContainer().get(kType, PersistentDataType.STRING);

        StringBuilder sb = new StringBuilder();
        sb.append("§eMonnTalismans: id=§f").append(id == null ? "(none)" : id);
        sb.append(" §etype=§f").append(type == null ? "(none)" : type);
        sb.append(" §7material=").append(it.getType());

        if (meta.hasAttributeModifiers()) {
            sb.append("\n§eAttributes:");
            for (var entry : meta.getAttributeModifiers().entries()) {
                sb.append("\n  §7").append(entry.getKey().name())
                  .append(": §f").append(entry.getValue().getAmount())
                  .append(" (").append(entry.getValue().getSlotGroup()).append(")");
            }
        }

        return sb.toString();
    }

    public String getItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(kId, PersistentDataType.STRING);
    }

    public ItemType getItemType(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String type = meta.getPersistentDataContainer().get(kType, PersistentDataType.STRING);
        if (type == null) return null;
        try {
            return ItemType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
