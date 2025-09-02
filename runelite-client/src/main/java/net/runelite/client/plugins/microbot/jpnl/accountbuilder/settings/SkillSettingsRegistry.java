package net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings;

import net.runelite.client.plugins.microbot.jpnl.accountbuilder.AllInOneConfig;
import net.runelite.client.plugins.microbot.jpnl.accountbuilder.enums.SkillType;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;
import static net.runelite.client.plugins.microbot.jpnl.accountbuilder.settings.SkillSettingsUtil.parseCsv;

/**
 * SkillSettingsRegistry
 *
 * Updated to work with the new AllInOneConfig structure with individual skill tabs.
 * Maps the new config method names to the runtime settings.
 */
public class SkillSettingsRegistry {

    private static final int NO_TARGET = -1;

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
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.attackStyle()))
                .useSpecial(bool(() -> config.attackUseSpec()))
                .build());

        map.put(SkillType.STRENGTH, SkillRuntimeSettings.builder()
                .skillType(SkillType.STRENGTH)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.strengthStyle()))
                .useSpecial(bool(() -> config.strengthUseSpec()))
                .build());

        map.put(SkillType.DEFENCE, SkillRuntimeSettings.builder()
                .skillType(SkillType.DEFENCE)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.defenceStyle()))
                .useSpecial(bool(() -> config.defenceUseSpec()))
                .build());

        map.put(SkillType.RANGED, SkillRuntimeSettings.builder()
                .skillType(SkillType.RANGED)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(str(() -> config.rangedAmmo()))
                .useSpecial(bool(() -> config.rangedUseSpec()))
                .build());

        map.put(SkillType.MAGIC, SkillRuntimeSettings.builder()
                .skillType(SkillType.MAGIC)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.magicMethod()))
                .build());

        map.put(SkillType.PRAYER, SkillRuntimeSettings.builder()
                .skillType(SkillType.PRAYER)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.prayerMethod()))
                .build());

        map.put(SkillType.HITPOINTS, SkillRuntimeSettings.builder()
                .skillType(SkillType.HITPOINTS)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(str(() -> config.hitpointsMethod()))
                .build());

        // ============ GATHERING ============
        Set<String> miningFlags = new HashSet<>();
        if (bool(() -> config.mining3Tick())) miningFlags.add("USE_3TICK");
        map.put(SkillType.MINING, SkillRuntimeSettings.builder()
                .skillType(SkillType.MINING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.miningMode()))
                .customList(parseCsv(str(() -> config.miningOres())))
                .flags(miningFlags)
                .build());

        map.put(SkillType.SMITHING, SkillRuntimeSettings.builder()
                .skillType(SkillType.SMITHING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.smithingMethod()))
                .build());

        Set<String> fishingFlags = new HashSet<>();
        fishingFlags.add("FISHING_METHOD:" + config.fishingMethod().name());
        map.put(SkillType.FISHING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FISHING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.fishingMode()))
                .useSpecial(bool(() -> config.fishingSpecial()))
                .flags(fishingFlags)
                .build());

        map.put(SkillType.COOKING, SkillRuntimeSettings.builder()
                .skillType(SkillType.COOKING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.cookingLocation()))
                .build());

        map.put(SkillType.FIREMAKING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FIREMAKING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.firemakingLogs()))
                .build());

        Set<String> wcFlags = new HashSet<>();
        if (bool(() -> config.woodcuttingNests())) wcFlags.add("PICKUP_NESTS");
        wcFlags.add("WC_TREE_TYPE:" + config.woodcuttingTrees().name());
        map.put(SkillType.WOODCUTTING, SkillRuntimeSettings.builder()
                .skillType(SkillType.WOODCUTTING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.woodcuttingMode()))
                .useSpecial(bool(() -> config.woodcuttingSpecial()))
                .flags(wcFlags)
                .build());

        // ============ ARTISAN ============
        map.put(SkillType.CRAFTING, SkillRuntimeSettings.builder()
                .skillType(SkillType.CRAFTING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.craftingMethod()))
                .build());

        map.put(SkillType.FLETCHING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FLETCHING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.fletchingMethod()))
                .build());

        map.put(SkillType.HERBLORE, SkillRuntimeSettings.builder()
                .skillType(SkillType.HERBLORE)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.herbloreMethod()))
                .build());

        Set<String> rcFlags = new HashSet<>();
        if (bool(() -> config.runecraftingPouches())) rcFlags.add("USE_POUCHES");
        if (bool(() -> config.runecraftingRepair())) rcFlags.add("REPAIR_POUCHES");
        map.put(SkillType.RUNECRAFTING, SkillRuntimeSettings.builder()
                .skillType(SkillType.RUNECRAFTING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.runecraftingMethod()))
                .flags(rcFlags)
                .build());

        map.put(SkillType.CONSTRUCTION, SkillRuntimeSettings.builder()
                .skillType(SkillType.CONSTRUCTION)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.constructionMethod()))
                .build());

        // ============ SUPPORT ============
        Set<String> agilityFlags = new HashSet<>();
        if (bool(() -> config.agilityStamina())) agilityFlags.add("USE_STAMINA");
        if (bool(() -> config.agilityMarks())) agilityFlags.add("PICKUP_MARKS");
        map.put(SkillType.AGILITY, SkillRuntimeSettings.builder()
                .skillType(SkillType.AGILITY)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.agilityCourse()))
                .flags(agilityFlags)
                .build());

        Set<String> thievingFlags = new HashSet<>();
        if (bool(() -> config.thievingDodgy())) thievingFlags.add("USE_DODGY");
        map.put(SkillType.THIEVING, SkillRuntimeSettings.builder()
                .skillType(SkillType.THIEVING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.thievingMethod()))
                .flags(thievingFlags)
                .build());

        Set<String> slayerFlags = new HashSet<>();
        if (bool(() -> config.slayerCannon())) slayerFlags.add("USE_CANNON");
        map.put(SkillType.SLAYER, SkillRuntimeSettings.builder()
                .skillType(SkillType.SLAYER)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.slayerStrategy()))
                .flags(slayerFlags)
                .build());

        map.put(SkillType.HUNTER, SkillRuntimeSettings.builder()
                .skillType(SkillType.HUNTER)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.hunterMethod()))
                .build());

        Set<String> farmingFlags = new HashSet<>();
        if (bool(() -> config.farmingBirdhouses())) farmingFlags.add("INCLUDE_BIRDHOUSES");
        map.put(SkillType.FARMING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FARMING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.farmingRunType()))
                .flags(farmingFlags)
                .build());

        this.cache = map;
    }

    public SkillRuntimeSettings get(SkillType skillType) {
        return cache.get(skillType);
    }

    // Helper methods
    private static String str(Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean bool(Supplier<Boolean> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return false;
        }
    }

    private static String enumName(Supplier<? extends Enum<?>> supplier) {
        try {
            Enum<?> e = supplier.get();
            return e != null ? e.name() : "";
        } catch (Exception ex) {
            return "";
        }
    }
}