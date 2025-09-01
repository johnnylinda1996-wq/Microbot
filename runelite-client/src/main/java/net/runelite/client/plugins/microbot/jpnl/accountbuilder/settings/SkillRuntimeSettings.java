package net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Set;

/**
 * Immutable snapshot van configuratie voor een skill.
 * Handlers mogen velden negeren die niet relevant zijn.
 */
@Value
@Builder(toBuilder = true)
public class SkillRuntimeSettings {
    net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.SkillType skillType;

    boolean enabled;
    int targetLevel;

    String mode;              // bijv. POWERFISH / BANK / POWERDROP etc.
    boolean useSpecial;
    boolean hopIfNoResource;

    // Generieke string-lijsten (drop list, custom rocks, etc.)
    @Builder.Default
    Set<String> customList = Collections.emptySet();

    // Placeholder voor toekomstige uitbreidingen (potionMode, prayerMode, flags, useStamina...)
    @Builder.Default
    Set<String> flags = Collections.emptySet();
}