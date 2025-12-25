package me.sumbiz.monntalismans.service;

import me.sumbiz.monntalismans.model.*;
import me.sumbiz.monntalismans.util.HeadTextureUtil;
import me.sumbiz.monntalismans.util.TextUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public ItemStack create(String id, int amount) {
        ItemDef def = items.get(id);
        if (def == null) return null;

        ItemStack base = buildTemplate(def);
        if (base == null) return null;

        applyCommonMeta(base, def);
        base.setAmount(Math.max(1, amount));
        return base;
    }

    private ItemStack buildTemplate(ItemDef itemDef) {
        TemplateDef t = (itemDef instanceof ItemDef.Sphere s) ? s.def().template() : ((ItemDef.Talisman)itemDef).def().template();

        // 1) если есть Nexo и указан fromNexoId — берём itemstack оттуда как “внешку”
        if (t.fromNexoId() != null && !t.fromNexoId().isBlank() && nexo.isAvailable()) {
            ItemStack nexoItem = nexo.buildItem(t.fromNexoId());
            if (nexoItem != null) return nexoItem;
        }

        // 2) fallback
        return new ItemStack(t.material());
    }

    private void applyCommonMeta(ItemStack item, ItemDef def) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (def instanceof ItemDef.Sphere s) {
            SphereDef sd = s.def();

            meta.displayName(TextUtil.parse(sd.name()));
            meta.lore(TextUtil.parseList(sd.lore()));

            // если это голова — применяем base64
            if (sd.headTextureBase64() != null && !sd.headTextureBase64().isBlank()) {
                // важно: применяем на item, а не на meta отдельно
                item.setItemMeta(meta);
                HeadTextureUtil.applyBase64IfSkull(item, sd.headTextureBase64());
                meta = item.getItemMeta();
                if (meta == null) return;
            }

            meta.getPersistentDataContainer().set(kId, PersistentDataType.STRING, sd.id());
            meta.getPersistentDataContainer().set(kType, PersistentDataType.STRING, ItemType.SPHERE.name());
            item.setItemMeta(meta);
            return;
        }

        if (def instanceof ItemDef.Talisman t) {
            TalismanDef td = t.def();

            meta.displayName(TextUtil.parse(td.name()));
            meta.lore(TextUtil.parseList(td.lore()));

            meta.getPersistentDataContainer().set(kId, PersistentDataType.STRING, td.id());
            meta.getPersistentDataContainer().set(kType, PersistentDataType.STRING, ItemType.TALISMAN.name());
            item.setItemMeta(meta);
        }
    }

    public String debugItemInHand(Player p) {
        ItemStack it = p.getInventory().getItemInMainHand();
        if (it == null || it.getType().isAir()) return "§eВ руке пусто.";

        ItemMeta meta = it.getItemMeta();
        if (meta == null) return "§eНет меты.";

        String id = meta.getPersistentDataContainer().get(kId, PersistentDataType.STRING);
        String type = meta.getPersistentDataContainer().get(kType, PersistentDataType.STRING);

        return "§eMonnTalismans: id=§f" + (id == null ? "(none)" : id) +
                " §etype=§f" + (type == null ? "(none)" : type) +
                " §7material=" + it.getType();
    }
}
