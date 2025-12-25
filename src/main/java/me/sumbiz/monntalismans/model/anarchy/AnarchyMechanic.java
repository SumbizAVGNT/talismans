package me.sumbiz.monntalismans.model.anarchy;

/**
 * Набор уникальных анархичных механик, которые можно включить для талисманов/сфер.
 */
public enum AnarchyMechanic {
    CHAOS_SHIELD("Резко снижает урон и накидывает Absorption при получении урона."),
    BLOOD_LINK("Вампиризм на нанесение урона и бонусный урон за комбо."),
    VOID_STEP("Рывок-телепорт при приседании, позволяющий скрытно сближаться."),
    RELIC_GUARD("Сохранение игрока от смерти за счёт редкой реликвии."),
    METEOR_CALL("Шанс вызвать мини-метеор по цели при ударе."),
    OVERCHARGE("Временный бафф скорости/силы при переключении предметов.");

    private final String description;

    AnarchyMechanic(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
