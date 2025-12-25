package me.sumbiz.monntalismans.model;

import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record TalismanDef(
        String nexoId,
        Set<ActivationSlot> activeIn,
        TalismanStackingMode stacking,
        List<PotionSpec> potions,
        Map<Attribute, Double> attributes,
        Map<PotionEffectType, Integer> potionAmpCaps,  // кап на усилитель по типу зелья (для SUM_TO_CAP)
        Map<Attribute, Double> attributeCaps           // кап на атрибут по типу (для SUM_TO_CAP)
) {
    /**
     * amplifierStep — сколько добавлять к amplifier за каждый дополнительный одинаковый талисман при SUM_TO_CAP.
     * По умолчанию 1 => если amplifier=0 и 3 талисмана, получится 0 + 2*1 = 2 (уровень III).
     */
    public record PotionSpec(
            PotionEffectType type,
            int amplifier,
            int amplifierStep,
            boolean ambient,
            boolean particles,
            boolean icon
    ) {}
}
