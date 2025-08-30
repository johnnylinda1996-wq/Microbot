package net.runelite.client.plugins.microbot.aiobot.settings;

import net.runelite.client.plugins.microbot.aiobot.AllInOneConfig;
import net.runelite.client.plugins.microbot.aiobot.enums.SkillType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;
import static net.runelite.client.plugins.microbot.aiobot.settings.SkillSettingsUtil.clampLevel;
import static net.runelite.client.plugins.microbot.aiobot.settings.SkillSettingsUtil.parseCsv;

/**
 * Bouwt een snapshot SkillType -> SkillRuntimeSettings.
 * Roep refresh() periodiek aan (bijv. elke 5s) of bij config wijzigingen.
 */
public class SkillSettingsRegistry {

    private final AllInOneConfig config;
    private volatile Map<SkillType, SkillRuntimeSettings> cache = new EnumMap<>(SkillType.class);

    public SkillSettingsRegistry(AllInOneConfig config) {
        this.config = config;
        refresh();
    }

    public void refresh() {
        EnumMap<SkillType, SkillRuntimeSettings> map = new EnumMap<>(SkillType.class);

        // ============ COMBAT ============
        map.put(SkillType.ATTACK, SkillRuntimeSettings.builder()
                .skillType(SkillType.ATTACK)
                .enabled(safeBool(() -> config.attackEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.attackTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.attackStyle()))
                .useSpecial(safeBool(() -> config.attackUseSpec()))
                .build());

        map.put(SkillType.STRENGTH, SkillRuntimeSettings.builder()
                .skillType(SkillType.STRENGTH)
                .enabled(safeBool(() -> config.strengthEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.strengthTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.strengthStyle()))
                .useSpecial(safeBool(() -> config.strengthUseSpec()))
                .build());

        map.put(SkillType.DEFENCE, SkillRuntimeSettings.builder()
                .skillType(SkillType.DEFENCE)
                .enabled(safeBool(() -> config.defenceEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.defenceTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.defenceStyle()))
                .useSpecial(safeBool(() -> config.defenceUseSpec()))
                .build());

        map.put(SkillType.RANGED, SkillRuntimeSettings.builder()
                .skillType(SkillType.RANGED)
                .enabled(safeBool(() -> config.rangedEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.rangedTargetLevel(), 1)))
                .mode(safeString(() -> config.rangedAmmoType()))
                .useSpecial(safeBool(() -> config.rangedUseSpec()))
                .build());

        map.put(SkillType.MAGIC, SkillRuntimeSettings.builder()
                .skillType(SkillType.MAGIC)
                .enabled(safeBool(() -> config.magicEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.magicTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.magicTrainingMode()))
                // Stamina flag voorbeeld
                .flags(flagIfTrue(safeBool(() -> config.magicUseStamina()), "USE_STAMINA"))
                .build());

        map.put(SkillType.PRAYER, SkillRuntimeSettings.builder()
                .skillType(SkillType.PRAYER)
                .enabled(safeBool(() -> config.prayerEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.prayerTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.prayerTrainingMode()))
                .build());

        map.put(SkillType.HITPOINTS, SkillRuntimeSettings.builder()
                .skillType(SkillType.HITPOINTS)
                .enabled(safeBool(() -> config.hitpointsEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.hitpointsTargetLevel(), 1)))
                .mode(safeString(() -> config.hitpointsMethod()))
                .build());

        // ============ GATHERING ============
        map.put(SkillType.MINING, SkillRuntimeSettings.builder()
                .skillType(SkillType.MINING)
                .enabled(safeBool(() -> config.miningEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.miningTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.miningMode()))
                .customList(parseCsv(safeString(() -> config.miningCustomRocks())))
                .build());

        map.put(SkillType.WOODCUTTING, SkillRuntimeSettings.builder()
                .skillType(SkillType.WOODCUTTING)
                .enabled(safeBool(() -> config.wcEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.wcTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.wcMode()))
                .useSpecial(safeBool(() -> config.wcUseSpec()))
                .build());

        map.put(SkillType.FISHING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FISHING)
                .enabled(safeBool(() -> config.fishingEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.fishingTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.fishingMode()))
                .useSpecial(safeBool(() -> config.fishingUseSpecHarpoon()))
                .customList(parseCsv(safeString(() -> config.fishingCustomDropList())))
                .hopIfNoResource(safeBool(() -> config.fishingHopIfNoSpot()))
                .build());

        map.put(SkillType.HUNTER, SkillRuntimeSettings.builder()
                .skillType(SkillType.HUNTER)
                .enabled(safeBool(() -> config.hunterEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.hunterTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.hunterMethod()))
                .build());

        map.put(SkillType.FARMING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FARMING)
                .enabled(safeBool(() -> config.farmingEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.farmingTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.farmingRunMode()))
                .build());

        // ============ ARTISAN ============
        map.put(SkillType.SMITHING, SkillRuntimeSettings.builder()
                .skillType(SkillType.SMITHING)
                .enabled(safeBool(() -> config.smithingEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.smithingTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.smithingMode()))
                .build());

        map.put(SkillType.FLETCHING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FLETCHING)
                .enabled(safeBool(() -> config.fletchingEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.fletchingTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.fletchingMode()))
                .build());

        map.put(SkillType.CRAFTING, SkillRuntimeSettings.builder()
                .skillType(SkillType.CRAFTING)
                .enabled(safeBool(() -> config.craftingEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.craftingTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.craftingMethod()))
                .build());

        map.put(SkillType.COOKING, SkillRuntimeSettings.builder()
                .skillType(SkillType.COOKING)
                .enabled(safeBool(() -> config.cookingEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.cookingTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.cookingMode()))
                .build());

        map.put(SkillType.FIREMAKING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FIREMAKING)
                .enabled(safeBool(() -> config.fmEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.fmTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.fmMode()))
                .flags(flagIfTrue(safeBool(() -> config.fmUseStaminas()), "USE_STAMINA"))
                .build());

        map.put(SkillType.HERBLORE, SkillRuntimeSettings.builder()
                .skillType(SkillType.HERBLORE)
                .enabled(safeBool(() -> config.herbloreEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.herbloreTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.herbloreMode()))
                .build());

        map.put(SkillType.RUNECRAFTING, SkillRuntimeSettings.builder()
                .skillType(SkillType.RUNECRAFTING)
                .enabled(safeBool(() -> config.rcEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.rcTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.rcMethod()))
                .build());

        map.put(SkillType.CONSTRUCTION, SkillRuntimeSettings.builder()
                .skillType(SkillType.CONSTRUCTION)
                .enabled(safeBool(() -> config.constructionEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.constructionTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.constructionMethod()))
                .build());

        // ============ MISC / SUPPORT ============
        map.put(SkillType.SLAYER, SkillRuntimeSettings.builder()
                .skillType(SkillType.SLAYER)
                .enabled(safeBool(() -> config.slayerEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.slayerTargetLevel(), 1)))
                // .mode(safeString(() -> config.slayerTaskStrategy())) // uncomment als getter bestaat
                .build());

        map.put(SkillType.THIEVING, SkillRuntimeSettings.builder()
                .skillType(SkillType.THIEVING)
                .enabled(safeBool(() -> config.thievingEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.thievingTargetLevel(), 1)))
                .mode(safeEnumName(() -> config.thievingMethod()))
                .build());

        map.put(SkillType.AGILITY, SkillRuntimeSettings.builder()
                .skillType(SkillType.AGILITY)
                .enabled(safeBool(() -> config.agilityEnabled()))
                .targetLevel(clampLevel(safeInt(() -> config.agilityTargetLevel(), 1)))
                // Kies de juiste getter die je werkelijk hebt:
                // .mode(safeEnumName(() -> config.agilityCourse()))
                // of:
                // .mode(safeEnumName(() -> config.agilityMode()))
                .flags(flagIfTrue(safeBool(() -> config.agilityUseStamina()), "USE_STAMINA"))
                .build());

        cache = map;
    }

    public SkillRuntimeSettings get(SkillType type) {
        return cache.get(type);
    }

    /* ================= Helpers ================= */

    private boolean safeBool(Supplier<Boolean> sup) {
        try { return sup.get(); } catch (Exception e) { return false; }
    }

    private int safeInt(Supplier<Integer> sup, int def) {
        try { return sup.get(); } catch (Exception e) { return def; }
    }

    private String safeString(Supplier<String> sup) {
        try {
            String s = sup.get();
            return (s == null || s.isBlank()) ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeEnumName(Supplier<? extends Enum<?>> sup) {
        try {
            Enum<?> e = sup.get();
            return e == null ? null : e.name();
        } catch (Exception ex) {
            return null;
        }
    }

    private Set<String> flagIfTrue(boolean cond, String flag) {
        return cond ? Set.of(flag) : emptySet();
    }
}