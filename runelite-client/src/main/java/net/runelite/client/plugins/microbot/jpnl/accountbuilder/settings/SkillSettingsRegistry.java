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
 * Aangepast na verwijderen van *Enabled / *TargetLevel getters uit AllInOneConfig:
 *  - enabled altijd true (GUI/queue bepaalt echte executie)
 *  - targetLevel = -1 (geen config target, gebruik taak target)
 *
 * Flags:
 *  - Wanneer meerdere flags nodig zijn (RC, Agility, etc.) worden ze nu samengevoegd
 *    in één Set voordat de builder wordt aangeroepen (i.p.v. een niet-bestaande flagsMerge()).
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
                .mode(str(() -> config.rangedAmmoType()))
                .useSpecial(bool(() -> config.rangedUseSpec()))
                .build());

        Set<String> magicFlags = new HashSet<>();
        if (bool(() -> config.magicUseStamina())) magicFlags.add("USE_STAMINA");
        map.put(SkillType.MAGIC, SkillRuntimeSettings.builder()
                .skillType(SkillType.MAGIC)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.magicTrainingMode()))
                .flags(magicFlags)
                .build());

        map.put(SkillType.PRAYER, SkillRuntimeSettings.builder()
                .skillType(SkillType.PRAYER)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.prayerTrainingMode()))
                .build());

        map.put(SkillType.HITPOINTS, SkillRuntimeSettings.builder()
                .skillType(SkillType.HITPOINTS)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(str(() -> config.hitpointsMethod()))
                .build());

        // ============ GATHERING ============
        Set<String> miningFlags = new HashSet<>();
        if (bool(() -> config.miningUse3Tick())) miningFlags.add("USE_3TICK");
        map.put(SkillType.MINING, SkillRuntimeSettings.builder()
                .skillType(SkillType.MINING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.miningMode()))
                .customList(parseCsv(str(() -> config.miningCustomRocks())))
                .flags(miningFlags)
                .hopIfNoResource(bool(() -> config.miningHopIfNoRock()))
                .build());

        Set<String> wcFlags = new HashSet<>();
        if (bool(() -> config.wcBirdNestPickup())) wcFlags.add("PICKUP_NESTS");
        wcFlags.add("WC_TREE_TYPE:" + config.wcTreeType().name());
        map.put(SkillType.WOODCUTTING, SkillRuntimeSettings.builder()
                .skillType(SkillType.WOODCUTTING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.wcMode()))
                .useSpecial(bool(() -> config.wcUseSpec()))
                .flags(wcFlags)
                .build());

        Set<String> fishingFlags = new HashSet<>();
        fishingFlags.add("FISHING_METHOD:" + config.fishingMethod().name());

        map.put(SkillType.FISHING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FISHING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.fishingMode()))
                .useSpecial(bool(() -> config.fishingUseSpecHarpoon()))
                .flags(fishingFlags)
                .build());

        map.put(SkillType.HUNTER, SkillRuntimeSettings.builder()
                .skillType(SkillType.HUNTER)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.hunterMethod()))
                .build());

        Set<String> farmingFlags = new HashSet<>();
        if (bool(() -> config.farmingBirdHouse())) farmingFlags.add("INCLUDE_BIRDHOUSES");
        map.put(SkillType.FARMING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FARMING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.farmingRunMode()))
                .flags(farmingFlags)
                .build());

        // ============ ARTISAN / PROCESSING ============
        Set<String> smithFlags = new HashSet<>();
        if (bool(() -> config.smithingUseCoalBag())) smithFlags.add("USE_COAL_BAG");
        map.put(SkillType.SMITHING, SkillRuntimeSettings.builder()
                .skillType(SkillType.SMITHING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.smithingMode()))
                .customList(parseCsv(str(() -> config.smithingBarType())))
                .flags(smithFlags)
                .build());

        map.put(SkillType.FLETCHING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FLETCHING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.fletchingMode()))
                .build());

        Set<String> craftingFlags = new HashSet<>();
        if (bool(() -> config.craftingUsePortable())) craftingFlags.add("USE_PORTABLE");
        map.put(SkillType.CRAFTING, SkillRuntimeSettings.builder()
                .skillType(SkillType.CRAFTING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.craftingMethod()))
                .flags(craftingFlags)
                .build());

        Set<String> cookingFlags = new HashSet<>();
        if (bool(() -> config.cookingGauntlets())) cookingFlags.add("COOKING_GAUNTLETS");
        map.put(SkillType.COOKING, SkillRuntimeSettings.builder()
                .skillType(SkillType.COOKING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.cookingMode()))
                .flags(cookingFlags)
                .build());

        Set<String> fmFlags = new HashSet<>();
        fmFlags.add("FM_MODE:" + enumName(() -> config.fmMode()));
        map.put(SkillType.FIREMAKING, SkillRuntimeSettings.builder()
                .skillType(SkillType.FIREMAKING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.fmMode()))
                .flags(fmFlags)
                .build());

        Set<String> herbloreFlags = new HashSet<>();
        if (bool(() -> config.herbloreUseSecondaries())) herbloreFlags.add("USE_SECONDARIES");
        map.put(SkillType.HERBLORE, SkillRuntimeSettings.builder()
                .skillType(SkillType.HERBLORE)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.herbloreMode()))
                .flags(herbloreFlags)
                .build());

        Set<String> rcFlags = new HashSet<>();
        if (bool(() -> config.rcUsePouches())) rcFlags.add("USE_POUCHES");
        if (bool(() -> config.rcRepairWithNpc())) rcFlags.add("NPC_REPAIR");
        map.put(SkillType.RUNECRAFTING, SkillRuntimeSettings.builder()
                .skillType(SkillType.RUNECRAFTING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.rcMethod()))
                .flags(rcFlags)
                .build());

        Set<String> conFlags = new HashSet<>();
        if (bool(() -> config.constructionUseServant())) conFlags.add("USE_SERVANT");
        map.put(SkillType.CONSTRUCTION, SkillRuntimeSettings.builder()
                .skillType(SkillType.CONSTRUCTION)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.constructionMethod()))
                .flags(conFlags)
                .build());

        // ============ MISC ============
        Set<String> slayerFlags = new HashSet<>();
        if (bool(() -> config.slayerUseCannon())) slayerFlags.add("USE_CANNON");
        map.put(SkillType.SLAYER, SkillRuntimeSettings.builder()
                .skillType(SkillType.SLAYER)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.slayerTaskStrategy()))
                .flags(slayerFlags)
                .build());

        Set<String> thievingFlags = new HashSet<>();
        if (bool(() -> config.thievingUseDodgy())) thievingFlags.add("USE_DODGY");
        map.put(SkillType.THIEVING, SkillRuntimeSettings.builder()
                .skillType(SkillType.THIEVING)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.thievingMethod()))
                .flags(thievingFlags)
                .build());

        Set<String> agilityFlags = new HashSet<>();
        if (bool(() -> config.agilityUseStamina())) agilityFlags.add("USE_STAMINA");
        if (bool(() -> config.agilityLootMarks())) agilityFlags.add("LOOT_MARKS");
        map.put(SkillType.AGILITY, SkillRuntimeSettings.builder()
                .skillType(SkillType.AGILITY)
                .enabled(true)
                .targetLevel(NO_TARGET)
                .mode(enumName(() -> config.agilityCourseMode()))
                .flags(agilityFlags)
                .build());

        cache = map;
    }

    public SkillRuntimeSettings get(SkillType type) {
        return cache.get(type);
    }

    /* ================= Helpers ================= */

    private boolean bool(Supplier<Boolean> sup) {
        try { return sup.get(); } catch (Exception e) { return false; }
    }

    private String str(Supplier<String> sup) {
        try {
            String s = sup.get();
            return (s == null || s.isBlank()) ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    private String enumName(Supplier<? extends Enum<?>> sup) {
        try {
            Enum<?> e = sup.get();
            return e == null ? null : e.name();
        } catch (Exception ex) {
            return null;
        }
    }

    private Set<String> flagIf(boolean cond, String flag) {
        return cond ? Set.of(flag) : emptySet();
    }
}