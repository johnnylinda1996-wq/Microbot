package net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Kleine utility om parsing/clamping te centraliseren.
 */
public final class SkillSettingsUtil {
    private SkillSettingsUtil() {}

    public static int clampLevel(int lvl) {
        if (lvl < 1) return 1;
        if (lvl > 120) return 120;
        return lvl;
    }

    public static Set<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptySet();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String enumName(Enum<?> e) {
        return e == null ? null : e.name();
    }
}