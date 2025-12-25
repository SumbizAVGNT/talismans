package me.sumbiz.moontalismans.mechanics;

/**
 * Типы уникальных механик для талисманов и сфер.
 * Каждая механика имеет уникальное поведение и может быть настроена индивидуально.
 */
public enum MechanicType {
    // ========== ОРИГИНАЛЬНЫЕ МЕХАНИКИ ==========

    /** Возрождение при смерти с восстановлением здоровья */
    REVIVE_ON_DEATH,

    /** Регенерация при низком здоровье */
    LOW_HEALTH_REGEN,

    /** Пассивная постоянная регенерация */
    PASSIVE_REGEN,

    /** Водное дыхание под водой */
    WATER_BREATHING,

    /** Ускорение при получении урона */
    SPEED_ON_DAMAGE,

    /** Ускорение в бою */
    COMBAT_SPEED,

    /** Уменьшение получаемого урона */
    DAMAGE_REDUCTION,

    /** Пассивная огнестойкость */
    FIRE_RESISTANCE,

    /** Сила при низком здоровье */
    LOW_HEALTH_STRENGTH,

    /** Пассивное поглощение урона */
    ABSORPTION,

    /** Пассивное насыщение */
    SATURATION,

    /** Шанс отравить цель при ударе */
    POISON_ON_HIT,

    /** Вампиризм - восстановление здоровья от урона */
    LIFESTEAL,

    /** Бонус к критическому урону */
    CRITICAL_DAMAGE_BOOST,

    /** Шанс замедлить цель при ударе */
    SLOWNESS_ON_HIT,

    /** Шанс оглушить цель (mining fatigue + nausea) */
    STUN_ON_HIT,

    /** Случайные негативные эффекты при ударе */
    RANDOM_DEBUFF_ON_HIT,

    /** AOE ослабление противников */
    AOE_WEAKNESS,

    /** Дополнительная защита при получении урона */
    RESISTANCE_ON_HIT,

    /** Отражение урона атакующему */
    DAMAGE_REFLECT,

    // ========== 20 НОВЫХ КРУТЫХ МЕХАНИК ==========

    /** Взрыв при смерти, наносящий урон врагам вокруг */
    EXPLOSION_ON_DEATH,

    /** Телепортация назад при критическом уроне */
    EMERGENCY_TELEPORT,

    /** Призыв молнии при критическом ударе */
    LIGHTNING_STRIKE,

    /** Вампиризм через AOE - восстановление от урона всем врагам рядом */
    AOE_LIFESTEAL,

    /** Шанс поджечь врага при ударе */
    FIRE_ON_HIT,

    /** Заморозка врага (замедление + mining fatigue) */
    FREEZE_ON_HIT,

    /** Невидимость при приседании */
    INVISIBILITY_ON_SNEAK,

    /** Двойной прыжок / бонус к прыжку */
    ENHANCED_JUMP,

    /** Шипы - урон атакующему при получении урона */
    THORNS_DAMAGE,

    /** Шанс уклонения от атаки */
    DODGE_CHANCE,

    /** Кража эффектов у цели */
    STEAL_EFFECTS,

    /** Распространение своих эффектов на союзников */
    AURA_BUFF_ALLIES,

    /** Лечение союзников при убийстве */
    HEAL_ALLIES_ON_KILL,

    /** Регенерация маны/опыта со временем */
    EXPERIENCE_GAIN,

    /** Шанс удвоить дроп с мобов */
    DOUBLE_DROPS,

    /** Бессмертие на короткое время при низком HP */
    LAST_STAND,

    /** Щит поглощения при блоке */
    SHIELD_ON_BLOCK,

    /** Ядовитая аура вокруг игрока */
    POISON_AURA,

    /** Восстановление прочности предметов */
    REPAIR_EQUIPMENT,

    /** Призыв союзников (волки/големы) */
    SUMMON_ALLIES,

    /** Кража здоровья у союзников для себя (тёмная механика) */
    BLOOD_SACRIFICE,

    /** Берсерк - больше урона при низком HP */
    BERSERKER_MODE,

    /** Цепная молния при ударе */
    CHAIN_LIGHTNING,

    /** Временная неуязвимость после возрождения */
    POST_REVIVE_INVULNERABILITY,

    /** Эффект массового страха (мобы убегают) */
    FEAR_AURA,

    /** Притяжение дропа к игроку */
    ITEM_MAGNET,

    /** Шанс игнорировать броню цели */
    ARMOR_PENETRATION,

    /** Кража части брони у цели */
    ARMOR_SHRED,

    /** Вурдалак - восстановление при убийстве */
    HEAL_ON_KILL,

    /** Перенаправление урона на атакующего */
    DAMAGE_REDIRECT;

    /**
     * Проверяет, является ли эта механика пассивной (постоянно активной).
     */
    public boolean isPassive() {
        return switch (this) {
            case PASSIVE_REGEN, WATER_BREATHING, FIRE_RESISTANCE, ABSORPTION,
                 SATURATION, INVISIBILITY_ON_SNEAK, ENHANCED_JUMP, POISON_AURA,
                 EXPERIENCE_GAIN, ITEM_MAGNET, REPAIR_EQUIPMENT -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, срабатывает ли механика при атаке.
     */
    public boolean triggersOnAttack() {
        return switch (this) {
            case POISON_ON_HIT, LIFESTEAL, CRITICAL_DAMAGE_BOOST, SLOWNESS_ON_HIT,
                 STUN_ON_HIT, RANDOM_DEBUFF_ON_HIT, AOE_WEAKNESS, AOE_LIFESTEAL,
                 FIRE_ON_HIT, FREEZE_ON_HIT, STEAL_EFFECTS, LIGHTNING_STRIKE,
                 CHAIN_LIGHTNING, ARMOR_PENETRATION, ARMOR_SHRED -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, срабатывает ли механика при получении урона.
     */
    public boolean triggersOnDamage() {
        return switch (this) {
            case SPEED_ON_DAMAGE, DAMAGE_REDUCTION, RESISTANCE_ON_HIT,
                 DAMAGE_REFLECT, THORNS_DAMAGE, DODGE_CHANCE, EMERGENCY_TELEPORT,
                 SHIELD_ON_BLOCK, DAMAGE_REDIRECT -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, срабатывает ли механика при смерти.
     */
    public boolean triggersOnDeath() {
        return switch (this) {
            case REVIVE_ON_DEATH, EXPLOSION_ON_DEATH -> true;
            default -> false;
        };
    }
}
