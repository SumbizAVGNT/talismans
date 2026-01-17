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

    /** Периодическое восстановление голода */
    PERIODIC_FEED,

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

    /** Тьма на атакующем при получении урона */
    DARKNESS_ON_HIT,

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
    DAMAGE_REDIRECT,

    // ========== 50 ДОПОЛНИТЕЛЬНЫХ УНИКАЛЬНЫХ МЕХАНИК ==========

    /** Привязка души к локации для телепортации обратно */
    SOUL_BIND,

    /** Щит из маны/опыта, поглощающий урон за счёт опыта */
    MANA_SHIELD,

    /** Отражение снарядов обратно в стрелка */
    REFLECT_PROJECTILES,

    /** Прохождение сквозь стены на короткое время */
    PHASE_THROUGH_WALLS,

    /** Замедление времени вокруг игрока */
    TIME_SLOW,

    /** Гравитационный колодец - притяжение врагов к игроку */
    GRAVITY_WELL,

    /** Рывок с уроном по направлению взгляда */
    DASH_ATTACK,

    /** Усиление силы в ночное время (Кровавая Луна) */
    BLOOD_MOON,

    /** Усиление защиты и регенерации днём (Солнечная Вспышка) */
    SOLAR_FLARE,

    /** Иммунитет к стихийным типам урона (огонь, молния, магия) */
    ELEMENTAL_IMMUNITY,

    /** Обмен здоровья на увеличенный урон */
    LIFE_TAP,

    /** Сбор душ с убитых врагов для усиления */
    SOUL_HARVEST,

    /** Некромантия - призыв зомби из убитых мобов */
    NECROMANCY,

    /** Случайная телепортация вокруг при атаке */
    PHASE_SHIFT,

    /** Создание иллюзорных клонов игрока */
    MIRROR_IMAGE,

    /** Мгновенная телепортация в точку взгляда */
    BLINK,

    /** Прохождение сквозь мобов без столкновений */
    SPIRIT_WALK,

    /** Удар бездны - урон игнорирующий ВСЕ защиты */
    VOID_STRIKE,

    /** Хаотический урон - случайный тип стихии при ударе */
    CHAOS_DAMAGE,

    /** Проклятие врага (случайные дебаффы со временем) */
    CURSE_ON_HIT,

    /** Священная кара - повышенный урон нежити и иллагерам */
    HOLY_SMITE,

    /** Тёмный договор - сила за счёт постепенной потери HP */
    DARK_PACT,

    /** Ярость при убийстве - стаки урона */
    RAGE_MODE,

    /** Комбо-урон - каждый удар сильнее предыдущего */
    COMBO_DAMAGE,

    /** Контратака после успешного блока щитом */
    COUNTER_ATTACK,

    /** Вихревая атака - урон всем вокруг при вращении */
    WHIRLWIND,

    /** Удар по земле с АОЕ уроном и отбрасыванием */
    GROUND_SLAM,

    /** Заряжаемая атака - накопление силы перед ударом */
    CHARGE_ATTACK,

    /** Добивание - мгновенное убийство врагов с низким HP */
    EXECUTE,

    /** Рассечение - урон по нескольким целям одновременно */
    CLEAVE,

    /** Кровотечение - урон со временем */
    BLEED_ON_HIT,

    /** Иммунитет к эффекту отравления */
    POISON_IMMUNITY,

    /** Иммунитет к эффекту иссушения */
    WITHER_IMMUNITY,

    /** Иммунитет к эффекту замедления */
    SLOWNESS_IMMUNITY,

    /** Иммунитет к эффекту слабости */
    WEAKNESS_IMMUNITY,

    /** Иммунитет к отбрасыванию */
    KNOCKBACK_IMMUNITY,

    /** Нет урона от падения */
    FALL_DAMAGE_IMMUNITY,

    /** Иммунитет к урону от взрывов */
    EXPLOSION_IMMUNITY,

    /** Отклонение снарядов в сторону */
    PROJECTILE_DEFLECT,

    /** Магический барьер - поглощение магического урона */
    MAGIC_BARRIER,

    /** Рунный щит - временный щит при активации */
    RUNE_SHIELD,

    /** Поглощение стихийного урона для лечения */
    ELEMENTAL_ABSORPTION,

    /** Призрачная форма - неуязвимость но нельзя атаковать */
    SPECTRAL_FORM,

    /** Ледяная нова - замораживание всех вокруг */
    FROST_NOVA,

    /** Огненный взрыв вокруг игрока */
    FLAME_BURST,

    /** Ударная волна отбрасывающая врагов */
    SHOCK_WAVE,

    /** Землетрясение - урон и замедление на земле */
    EARTHQUAKE,

    /** Призыв торнадо наносящего урон */
    TORNADO,

    /** Призыв метеора с неба */
    METEOR_STRIKE,

    /** Божественное вмешательство - спасение от смерти */
    DIVINE_INTERVENTION,

    /** Превращение в демона - сила за счёт контроля */
    DEMON_TRANSFORMATION,

    /** Крылья ангела - возможность полёта */
    ANGEL_WINGS;

    /**
     * Проверяет, является ли эта механика пассивной (постоянно активной).
     */
    public boolean isPassive() {
        return switch (this) {
            case PASSIVE_REGEN, LOW_HEALTH_REGEN, WATER_BREATHING, FIRE_RESISTANCE,
                 ABSORPTION, SATURATION, PERIODIC_FEED, LOW_HEALTH_STRENGTH, INVISIBILITY_ON_SNEAK,
                 ENHANCED_JUMP, POISON_AURA, EXPERIENCE_GAIN, ITEM_MAGNET,
                 REPAIR_EQUIPMENT, BLOOD_MOON, SOLAR_FLARE, ELEMENTAL_IMMUNITY,
                 SPIRIT_WALK, POISON_IMMUNITY, WITHER_IMMUNITY, SLOWNESS_IMMUNITY, WEAKNESS_IMMUNITY,
                 KNOCKBACK_IMMUNITY, FALL_DAMAGE_IMMUNITY, EXPLOSION_IMMUNITY,
                 MAGIC_BARRIER, ANGEL_WINGS, DARK_PACT -> true;
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
                 CHAIN_LIGHTNING, ARMOR_PENETRATION, ARMOR_SHRED, PHASE_SHIFT,
                 VOID_STRIKE, CHAOS_DAMAGE, CURSE_ON_HIT, HOLY_SMITE, COMBO_DAMAGE,
                 WHIRLWIND, CLEAVE, BLEED_ON_HIT, LIFE_TAP, EXECUTE -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, срабатывает ли механика при получении урона.
     */
    public boolean triggersOnDamage() {
        return switch (this) {
            case SPEED_ON_DAMAGE, DAMAGE_REDUCTION, RESISTANCE_ON_HIT,
                 DAMAGE_REFLECT, THORNS_DAMAGE, DARKNESS_ON_HIT, DODGE_CHANCE, EMERGENCY_TELEPORT,
                 SHIELD_ON_BLOCK, DAMAGE_REDIRECT, MANA_SHIELD, REFLECT_PROJECTILES,
                 TIME_SLOW, RUNE_SHIELD, ELEMENTAL_ABSORPTION, SPECTRAL_FORM,
                 FROST_NOVA, SHOCK_WAVE, COUNTER_ATTACK, PROJECTILE_DEFLECT -> true;
            default -> false;
        };
    }

    /**
     * Проверяет, срабатывает ли механика при смерти.
     */
    public boolean triggersOnDeath() {
        return switch (this) {
            case REVIVE_ON_DEATH, EXPLOSION_ON_DEATH, DIVINE_INTERVENTION -> true;
            default -> false;
        };
    }
}
