package me.sumbiz.monntalismans.model;

/**
 * Общий интерфейс для определений предметов (талисманов и сфер).
 */
public sealed interface ItemDef permits ItemDef.Talisman, ItemDef.Sphere {

    ItemType type();
    String id();
    boolean enabled();

    record Talisman(TalismanDef def) implements ItemDef {
        @Override public ItemType type() { return ItemType.TALISMAN; }
        @Override public String id() { return def.id(); }
        @Override public boolean enabled() { return def.enabled(); }
    }

    record Sphere(SphereDef def) implements ItemDef {
        @Override public ItemType type() { return ItemType.SPHERE; }
        @Override public String id() { return def.id(); }
        @Override public boolean enabled() { return def.enabled(); }
    }

    static ItemDef talisman(TalismanDef def) { return new Talisman(def); }
    static ItemDef sphere(SphereDef def) { return new Sphere(def); }
}
