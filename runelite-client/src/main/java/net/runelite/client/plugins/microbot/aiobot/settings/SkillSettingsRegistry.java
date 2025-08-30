package net.runelite.client.plugins.microbot.aiobot.settings;

import net.runelite.client.plugins.microbot.aiobot.AllInOneConfig;
import net.runelite.client.plugins.microbot.aiobot.enums.SkillType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Leest per-skill instellingen uit de config en maakt een snapshot
 * dat de handlers kunnen gebruiken.
 */
public class SkillSettingsRegistry {

    private final AllInOneConfig config;
    private volatile Map<SkillType, SkillRuntimeSettings> cache = new EnumMap<>(SkillType.class);

    public SkillSettingsRegistry(AllInOneConfig config) {
        this.config = config;
        refresh();
    }

    public void refresh() {
        Map<SkillType, SkillRuntimeSettings> map = new EnumMap<>(SkillType.class);

        // FISHING
        map.put(SkillType.FISHING,
                SkillRuntimeSettings.builder()
                        .skillType(SkillType.FISHING)
                        .enabled(safeBool(config.fishingEnabled()))
                        .targetLevel(safeLevel(config.fishingTargetLevel()))
                        .mode(config.fishingMode().name())
                        .customList(parseList(config.fishingCustomDropList()))
                        .useSpecial(config.fishingUseSpecHarpoon())
                        .hopIfNoResource(config.fishingHopIfNoSpot())
                        .build()
        );

        // MINING
        map.put(SkillType.MINING,
                SkillRuntimeSettings.builder()
                        .skillType(SkillType.MINING)
                        .enabled(safeBool(config.miningEnabled()))
                        .targetLevel(safeLevel(config.miningTargetLevel()))
                        .mode(config.miningMode().name())
                        .customList(parseList(config.miningCustomRocks()))
                        .useSpecial(false) // placeholder
                        .hopIfNoResource(false)
                        .build()
        );

        // Voeg later andere skills toe...
        this.cache = map;
    }

    private boolean safeBool(boolean b) {
        return b;
    }

    private int safeLevel(int lvl) {
        if (lvl < 1) return 1;
        if (lvl > 120) return 120;
        return lvl;
    }

    private Set<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptySet();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public SkillRuntimeSettings get(SkillType type) {
        return cache.get(type);
    }
}