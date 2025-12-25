# MoonTalismans

Плагин для создания талисманов и сфер с атрибутами, эффектами зелий и уникальными механиками для анархийных серверов.

## Требования

- Paper 1.20.6+
- Java 21+
- (Опционально) Nexo для кастомных текстур

## Команды

| Команда | Описание |
|---------|----------|
| `/talismans give <id> [amount] [player]` | Выдать предмет |
| `/talismans list` | Список всех предметов |
| `/talismans reload` | Перезагрузить конфигурацию |
| `/talismans debug` | Показать информацию о предмете в руке |
| `/talismans gui [page]` | Открыть админ-GUI для просмотра, выдачи и редактирования рецептов |

## Разрешения

| Разрешение | Описание |
|------------|----------|
| `moontalismans.admin` | Доступ ко всем командам |
| `moontalismans.give` | Выдача предметов |
| `moontalismans.reload` | Перезагрузка |

## Админ-GUI

- `/talismans gui` — открывает меню со списком всех сфер/талисманов с пагинацией (45 слотов на страницу)
- ЛКМ по предмету — моментально выдать себе копию
- ПКМ по предмету — открыть редактор рецепта
- Кнопка в центре — быстрый переход к созданию нового предмета с шаблонным ID

### Редактор рецептов

Редактор работает для shapeless-рецептов прямо из игры.

1. Откройте `/talismans gui` и нажмите ПКМ по предмету или кнопку «Создать предмет»
2. Положите в первые 9 слотов любые предметы — они будут сохранены как материалы рецепта
3. Нажмите «Сохранить рецепт» (или просто закройте инвентарь — сохранение произойдёт автоматически)
4. Плагин перезапишет рецепт в `config.yml` и зарегистрирует его заново без рестарта сервера

Новый предмет получает временный ID вида `custom_<timestamp>`; при необходимости настройте остальные параметры в `config.yml` вручную.

---

# Конфигурация config.yml

## Базовая структура

```yaml
info:
  namespace: my_talismans  # Пространство имён для предметов

items:
  item_id:
    enabled: true                # Включен ли предмет
    display_name: "&eНазвание"   # Название предмета (поддерживает &, hex &#RRGGBB)
    lore:                        # Описание
      - "&7Строка 1"
      - "&9Строка 2"
    glint: true                  # Зачарованный блеск
    item_flags:                  # Флаги предмета
      - HIDE_ATTRIBUTES
      - HIDE_ENCHANTS
    resource:
      material: TOTEM_OF_UNDYING # Базовый материал
      generate: false            # Генерировать модель
      model_path: item/talisman  # Путь к модели
      from_nexo: nexo_item_id    # Использовать предмет Nexo как базу
```

---

## Атрибуты

### Доступные атрибуты

| Имя в конфиге | Описание |
|---------------|----------|
| `attackDamage`, `damage` | Урон |
| `attackSpeed` | Скорость атаки |
| `maxHealth`, `health` | Максимальное здоровье |
| `armor` | Броня |
| `armorToughness`, `toughness` | Твёрдость брони |
| `movementSpeed`, `speed` | Скорость передвижения |
| `luck` | Удача |
| `knockbackResistance` | Сопротивление отбрасыванию |
| `attackKnockback` | Отбрасывание при атаке |
| `flyingSpeed` | Скорость полёта |

### Слоты активации

| Слот | Описание |
|------|----------|
| `mainhand` | Основная рука |
| `offhand` | Вторая рука |
| `hand` | Любая рука |
| `head` | Шлем |
| `chest` | Нагрудник |
| `legs` | Поножи |
| `feet` | Ботинки |
| `armor` | Любой слот брони |
| `hotbar` | Хотбар |
| `inventory` | Инвентарь |
| `any` | Где угодно |

### Пример атрибутов

```yaml
attribute_modifiers:
  offhand:
    attackDamage: 5       # +5 урона
    maxHealth: -2         # -2 здоровья
    armor: 2              # +2 брони
    movementSpeed: 0.015  # +15% скорости
    attackSpeed: -0.025   # -25% скорости атаки
```

---

## Талисманы (TOTEM_OF_UNDYING)

Талисманы — пассивные предметы, которые дают эффекты когда находятся в определённом слоте.

### Пример талисмана

```yaml
items:
  talisman_warrior:
    enabled: true
    display_name: "&#FF5500Талисман Воина"
    lore:
      - ""
      - "&7Во второй руке:"
      - "&9+5 Урон"
      - "&c-2 Здоровья"
    glint: true
    item_flags:
      - HIDE_ATTRIBUTES
    resource:
      material: TOTEM_OF_UNDYING
      model_path: item/talisman_warrior
    attribute_modifiers:
      offhand:
        attackDamage: 5
        maxHealth: -2
```

---

## Сферы (PLAYER_HEAD)

Сферы — предметы на основе головы игрока с кастомной текстурой.

### Текстуры голов

Текстуры можно найти на [minecraft-heads.com](https://minecraft-heads.com).
Используйте значение из поля "Value" (base64 строка).

### Текстуры тотемов

1. Если у вас установлен Nexo, пропишите `resource.from_nexo` в разделе предмета (например, `from_nexo: my_totem_skin`) — модель и текстура подтянутся автоматически.
2. Для собственного ресурспака:
   - Создайте модель в `assets/<namespace>/models/item/<имя>.json` и укажите текстуру
   - В `resource.model_path` запишите путь до модели без `assets/` (например, `item/talisman_warrior`)
   - При необходимости включите `generate: true`, чтобы плагин использовал готовый CustomModelData из ресурспака
3. Перезагрузите ресурс-пак на клиенте и выполните `/talismans reload`.

### Пример сферы

```yaml
items:
  sphere_magic:
    enabled: true
    display_name: "&#00FFFFСфера Магии"
    lore:
      - ""
      - "&7Во второй руке:"
      - "&9+3 Урона"
    glint: true
    item_flags:
      - HIDE_ATTRIBUTES
    resource:
      material: PLAYER_HEAD
    head_texture_base64: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2RjYTcxZDdjMjlhMzJlMzZlMzVhZWYzOTgxMWQzYjhhYmZlMjQ3NzZhYzc3YTJjMzQyZTNhMWZiNTYifX19"
    attribute_modifiers:
      offhand:
        attackDamage: 3
```

---

## Уникальные механики для анархии

MoonTalismans включает уникальные механики, которых нет в других плагинах талисманов.

### Механики для сфер

#### Вампиризм (Vampirism)
Восстановление здоровья при нанесении урона.

```yaml
vampirism:
  percent: 0.15           # 15% урона восстанавливается как здоровье
  maxHealPerHit: 4.0      # Максимум 4 HP за удар
  onlyMelee: true         # Только ближний бой
```

#### Ярость (Rage)
Увеличение урона и скорости при низком здоровье.

```yaml
rage:
  healthThreshold: 0.3    # Активируется при <30% HP
  damageMultiplier: 1.5   # +50% урона
  speedBoost: 0.2         # +20% скорости
```

#### Скрытность (Stealth)
Невидимость при приседании.

```yaml
stealth:
  invisibleOnSneak: true  # Невидимость при Shift
  backstabMultiplier: 2.0 # x2 урона в спину
  noParticles: true       # Без частиц невидимости
```

#### Шипы (Thorns)
Отражение урона атакующему.

```yaml
thorns:
  reflectPercent: 0.25    # Отражать 25% урона
  flatReflect: 1.0        # +1 фиксированный урон отражения
  ignoreArmor: false      # Игнорировать броню врага
```

#### Кража жизни (Lifesteal)
Исцеление при убийстве.

```yaml
lifesteal:
  healOnKill: 8.0           # +8 HP при убийстве игрока
  healOnMobKill: 2.0        # +2 HP при убийстве моба
  tempMaxHealthBonus: 4.0   # Временный бонус +4 макс. HP
  tempDuration: 30000       # Длительность 30 секунд
```

---

### Механики для талисманов

#### Комбо (Combo)
Накопление ударов для бонусного урона.

```yaml
combo:
  hitsRequired: 5         # 5 ударов для комбо
  comboWindow: 3000       # Окно 3 секунды
  bonusDamage: 6.0        # +6 урона при комбо
  bonusEffect: SLOWNESS   # Эффект при комбо
  effectAmplifier: 1      # Уровень эффекта
  effectDuration: 2000    # 2 секунды эффекта
  resetOnMiss: false      # Сбрасывать при промахе
```

#### Казнь (Execute)
Мгновенное убийство при низком здоровье врага.

```yaml
execute:
  healthThreshold: 0.15   # Убить при <15% HP
  cooldownMs: 30000       # Кулдаун 30 секунд
  particleEffect: true    # Показывать частицы
  onlyPlayers: true       # Только игроки
```

#### Уклонение (Dodge)
Шанс полностью избежать урона.

```yaml
dodge:
  chance: 0.20            # 20% шанс уклонения
  cooldownMs: 5000        # Кулдаун 5 секунд
  speedBoostOnDodge: 0.3  # +30% скорости после уклонения
  speedBoostDuration: 2000 # 2 секунды
```

#### Берсерк (Berserker)
Усиление при получении урона.

```yaml
berserker:
  stacksPerHit: 1         # +1 стак за удар
  maxStacks: 10           # Максимум 10 стаков
  damagePerStack: 0.5     # +0.5 урона за стак
  stackDuration: 8000     # Стаки держатся 8 секунд
  attackSpeedPerStack: 0.02 # +2% скорости атаки за стак
```

#### Привязка души (Soulbound)
Предмет не выпадает при смерти.

```yaml
soulbound:
  keepOnDeath: true       # Сохранять при смерти
  curseOnSteal: false     # Проклятие при воровстве
  bindToPlayer: false     # Привязать к игроку
```

---

## Полный пример талисмана с механиками

```yaml
items:
  talisman_berserker:
    enabled: true
    display_name: "&#FF0000Талисман Берсерка"
    lore:
      - ""
      - "&7Во второй руке:"
      - "&9+4 Урон"
      - "&c-2 Брони"
      - ""
      - "&6Берсерк:"
      - "&7Получение урона усиливает атаки"
      - "&7До +5 урона за 10 стаков"
    glint: true
    item_flags:
      - HIDE_ATTRIBUTES
    resource:
      material: TOTEM_OF_UNDYING
    attribute_modifiers:
      offhand:
        attackDamage: 4
        armor: -2
    berserker:
      stacksPerHit: 1
      maxStacks: 10
      damagePerStack: 0.5
      stackDuration: 8000
      attackSpeedPerStack: 0.02
```

---

## Полный пример сферы с механиками

```yaml
items:
  sphere_vampire:
    enabled: true
    display_name: "&#8B0000Сфера Вампира"
    lore:
      - ""
      - "&7Во второй руке:"
      - "&9+3 Урон"
      - "&c-4 Здоровья"
      - ""
      - "&6Вампиризм:"
      - "&7Восстанавливает 20% урона как здоровье"
    glint: true
    item_flags:
      - HIDE_ATTRIBUTES
    resource:
      material: PLAYER_HEAD
    head_texture_base64: "eyJ0ZXh0dXJlcyI6..."
    attribute_modifiers:
      offhand:
        attackDamage: 3
        maxHealth: -4
    vampirism:
      percent: 0.20
      maxHealPerHit: 5.0
      onlyMelee: true
```

---

## Флаги предметов

| Флаг | Описание |
|------|----------|
| `HIDE_ATTRIBUTES` | Скрыть атрибуты |
| `HIDE_ENCHANTS` | Скрыть зачарования |
| `HIDE_DESTROYS` | Скрыть "Может сломать" |
| `HIDE_PLACED_ON` | Скрыть "Можно поставить на" |
| `HIDE_UNBREAKABLE` | Скрыть "Неразрушимо" |
| `HIDE_DYE` | Скрыть цвет красителя |
| `HIDE_ARMOR_TRIM` | Скрыть узоры брони |

---

## Лимиты

```yaml
limits:
  max_aoe_radius: 10        # Макс. радиус AoE эффектов
  default_cooldown: "30s"   # Кулдаун по умолчанию
  default_global_cooldown: "1s" # Глобальный кулдаун
```

---

## Интеграция с Nexo

Плагин автоматически определяет Nexo и позволяет использовать предметы Nexo как базу:

```yaml
items:
  my_talisman:
    resource:
      from_nexo: my_nexo_item_id  # ID предмета из Nexo
```

При использовании Nexo плагин также добавляет механики `talisman` и `sphere` в Nexo конфигурацию.

---

## Поддержка цветов

Плагин поддерживает:

- Стандартные коды: `&a`, `&b`, `&c` и т.д.
- Hex-цвета: `&#RRGGBB` (например `&#FF5500`)
- Градиенты через MiniMessage (если используется Adventure API)

---

## Примечания

1. **Атрибуты** применяются только когда предмет в указанном слоте
2. **Сферы** определяются по материалу `PLAYER_HEAD` или наличию `head_texture_base64`
3. **Механики анархии** работают только когда предмет в offhand (левой руке)
4. Значения атрибутов `movementSpeed` и `attackSpeed` задаются как десятичные дроби (0.01 = 1%)

---

## Лицензия

MIT License
