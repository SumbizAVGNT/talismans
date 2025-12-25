package me.sumbiz.monntalismans.model;

public sealed interface ItemDef permits ItemDef.Talisman, ItemDef.Sphere {
    ItemType type();

    record Talisman(TalismanDef def) implements ItemDef {
        @Override public ItemType type() { return ItemType.TALISMAN; }
    }

    record Sphere(SphereDef def) implements ItemDef {
        @Override public ItemType type() { return ItemType.SPHERE; }
    }

    static ItemDef talisman(TalismanDef def) { return new Talisman(def); }
    static ItemDef sphere(SphereDef def) { return new Sphere(def); }
}
