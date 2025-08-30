package net.runelite.client.plugins.microbot.aiobot.settings;

import lombok.Builder;
import lombok.Value;
import net.runelite.client.plugins.microbot.aiobot.enums.SkillType;

import java.util.Set;

@Value
@Builder
public class SkillRuntimeSettings {
    SkillType skillType;
    boolean enabled;
    int targetLevel;          // fallback target
    String mode;              // generic mode string (skill-specifiek interpreteren)
    Set<String> customList;   // drop list / filter / selection
    boolean useSpecial;       // bijv. spec harpoon / infernal axe / etc.
    boolean hopIfNoResource;  // bijv. fishing hop, mining hop
}