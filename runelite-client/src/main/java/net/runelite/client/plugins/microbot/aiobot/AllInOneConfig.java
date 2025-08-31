package net.runelite.client.plugins.microbot.aiobot;

import net.runelite.client.config.*;

/**
 * All-In-One configuration (gereduceerd):
 *
 * Verwijderd:
 *  - Alle *Enabled opties (worden nu via GUI / queue beslist)
 *  - Alle *TargetLevel opties (target levels worden nu per taak in de GUI gezet)
 *
 * Overgebleven:
 *  - Alleen settings die daadwerkelijk runtime gedrag beïnvloeden (styles, modes, toggles)
 *  - Algemene settings (antiban, debug, autostart)
 *
 * Let op:
 *  - Alle code die nog refereert aan bijv. config.attackEnabled() of config.attackTargetLevel()
 *    moet worden aangepast om de GUI/queue bron te gebruiken.
 */
@ConfigGroup("allInOneAio")
public interface AllInOneConfig extends Config {

    /* ===================== GENERAL ====================== */

    @ConfigSection(
            name = "General",
            description = "Algemene AIO instellingen",
            position = 0
    )
    String generalSection = "generalSection";

    @ConfigItem(
            keyName = "antibanEnabled",
            name = "Enable Antiban",
            description = "Gebruik antiban gedrag",
            section = generalSection,
            position = 0
    )
    default boolean antibanEnabled() { return true; }

    @ConfigItem(
            keyName = "debug",
            name = "Debug logging",
            description = "Extra console / log output",
            section = generalSection,
            position = 1
    )
    default boolean debug() { return false; }

    @ConfigItem(
            keyName = "autoStart",
            name = "Auto start script",
            description = "Start automatisch de loop bij plugin start",
            section = generalSection,
            position = 2
    )
    default boolean autoStart() { return false; }

    @ConfigItem(
            keyName = "queuePersistence",
            name = "Queue JSON (Intern)",
            description = "Intern veld voor queue persistentie",
            section = generalSection,
            position = 3,
            hidden = true
    )
    default String queuePersistence() { return ""; }

    /* =====================================================
       COMBAT SKILLS (Attack / Strength / Defence / Ranged / Magic / Prayer / Hitpoints)
       Verwijderd: enabled + targetLevel config items
       ===================================================== */

    @ConfigSection(
            name = "Attack",
            description = "Attack instellingen",
            position = 10,
            closedByDefault = true
    )
    String attackSection = "attackSection";

    @ConfigItem(keyName = "attackStyle", name = "Style", description = "Training stijl", section = attackSection, position = 0)
    default CombatStyle attackStyle() { return CombatStyle.ACCURATE; }

    @ConfigItem(keyName = "attackFoodBelow", name = "Eet onder HP%", description = "Eet voedsel onder dit HP %", section = attackSection, position = 1)
    default int attackFoodBelow() { return 40; }

    @ConfigItem(keyName = "attackUseSpec", name = "Gebruik Special", description = "Gebruik spec bij beschikbaarheid", section = attackSection, position = 2)
    default boolean attackUseSpec() { return true; }

    @ConfigItem(keyName = "attackPotionMode", name = "Potion Mode", description = "Gebruik combat potions", section = attackSection, position = 3)
    default PotionMode attackPotionMode() { return PotionMode.NONE; }

    /* Strength */
    @ConfigSection(
            name = "Strength",
            description = "Strength instellingen",
            position = 11,
            closedByDefault = true
    )
    String strengthSection = "strengthSection";

    @ConfigItem(keyName = "strengthStyle", name = "Style", description = "Training stijl", section = strengthSection, position = 0)
    default CombatStyle strengthStyle() { return CombatStyle.AGGRESSIVE; }

    @ConfigItem(keyName = "strengthFoodBelow", name = "Eet onder HP%", description = "HP threshold", section = strengthSection, position = 1)
    default int strengthFoodBelow() { return 40; }

    @ConfigItem(keyName = "strengthUseSpec", name = "Gebruik Special", description = "Gebruik spec", section = strengthSection, position = 2)
    default boolean strengthUseSpec() { return true; }

    @ConfigItem(keyName = "strengthPotionMode", name = "Potion Mode", description = "Gebruik potions", section = strengthSection, position = 3)
    default PotionMode strengthPotionMode() { return PotionMode.NONE; }

    /* Defence */
    @ConfigSection(
            name = "Defence",
            description = "Defence instellingen",
            position = 12,
            closedByDefault = true
    )
    String defenceSection = "defenceSection";

    @ConfigItem(keyName = "defenceStyle", name = "Style", description = "Training stijl", section = defenceSection, position = 0)
    default CombatStyle defenceStyle() { return CombatStyle.DEFENSIVE; }

    @ConfigItem(keyName = "defenceFoodBelow", name = "Eet onder HP%", description = "HP% eten", section = defenceSection, position = 1)
    default int defenceFoodBelow() { return 40; }

    @ConfigItem(keyName = "defenceUseSpec", name = "Gebruik Special", description = "Spec gebruiken", section = defenceSection, position = 2)
    default boolean defenceUseSpec() { return false; }

    @ConfigItem(keyName = "defencePotionMode", name = "Potion Mode", description = "Gebruik potions", section = defenceSection, position = 3)
    default PotionMode defencePotionMode() { return PotionMode.NONE; }

    /* Ranged */
    @ConfigSection(
            name = "Ranged",
            description = "Ranged instellingen",
            position = 13,
            closedByDefault = true
    )
    String rangedSection = "rangedSection";

    @ConfigItem(keyName = "rangedAmmoType", name = "Ammo Type", description = "Type munitie (placeholder)", section = rangedSection, position = 0)
    default String rangedAmmoType() { return ""; }

    @ConfigItem(keyName = "rangedPrayerMode", name = "Prayer Mode", description = "Bescherm / DPS prayer", section = rangedSection, position = 1)
    default PrayerSupportMode rangedPrayerMode() { return PrayerSupportMode.NONE; }

    @ConfigItem(keyName = "rangedUseSpec", name = "Gebruik Special", description = "Spec gebruiken (D Bow / ACB etc.)", section = rangedSection, position = 2)
    default boolean rangedUseSpec() { return false; }

    @ConfigItem(keyName = "rangedFoodBelow", name = "Eet onder HP%", description = "HP% eten", section = rangedSection, position = 3)
    default int rangedFoodBelow() { return 50; }

    /* Magic */
    @ConfigSection(
            name = "Magic",
            description = "Magic instellingen",
            position = 14,
            closedByDefault = true
    )
    String magicSection = "magicSection";

    @ConfigItem(keyName = "magicTrainingMode", name = "Training Mode", description = "Spell/strategie", section = magicSection, position = 0)
    default MagicTrainingMode magicTrainingMode() { return MagicTrainingMode.HIGH_ALCH; }

    @ConfigItem(keyName = "magicEnableSplashFailsafe", name = "Splash Failsafe", description = "Herstart bij geen XP tick", section = magicSection, position = 1)
    default boolean magicEnableSplashFailsafe() { return true; }

    @ConfigItem(keyName = "magicUseStamina", name = "Use Stamina", description = "Gebruik stamina potions", section = magicSection, position = 2)
    default boolean magicUseStamina() { return false; }

    /* Prayer */
    @ConfigSection(
            name = "Prayer",
            description = "Prayer instellingen",
            position = 15,
            closedByDefault = true
    )
    String prayerSection = "prayerSection";

    @ConfigItem(keyName = "prayerTrainingMode", name = "Mode", description = "Beenderen / Gilded / Ectofuntus (placeholder)", section = prayerSection, position = 0)
    default PrayerTrainingMode prayerTrainingMode() { return PrayerTrainingMode.BONES_ALTAR; }

    @ConfigItem(keyName = "prayerUseIncense", name = "Use Incense (future)", description = "Toekomstige buff support", section = prayerSection, position = 1)
    default boolean prayerUseIncense() { return false; }

    /* Hitpoints (passief) */
    @ConfigSection(
            name = "Hitpoints",
            description = "Hitpoints instellingen",
            position = 16,
            closedByDefault = true
    )
    String hpSection = "hpSection";

    @ConfigItem(keyName = "hitpointsMethod", name = "Method (placeholder)", description = "NMZ / Sandcrabs etc", section = hpSection, position = 0)
    default String hitpointsMethod() { return ""; }

    /* =====================================================
       GATHERING SKILLS (Mining / Woodcutting / Fishing / Hunter / Farming)
       ===================================================== */

    @ConfigSection(
            name = "Mining",
            description = "Mining instellingen",
            position = 20,
            closedByDefault = true
    )
    String miningSection = "miningSection";

    @ConfigItem(keyName = "miningMode", name = "Mode", description = "Powerdrop / Bank", section = miningSection, position = 0)
    default MiningMode miningMode() { return MiningMode.POWERDROP; }

    @ConfigItem(keyName = "miningCustomRocks", name = "Allowed Rocks", description = "comma (iron,coal,gold)", section = miningSection, position = 1)
    default String miningCustomRocks() { return ""; }

    @ConfigItem(keyName = "miningUse3Tick", name = "Enable 3-tick", description = "Toekomstige geavanceerde methode", section = miningSection, position = 2)
    default boolean miningUse3Tick() { return false; }

    @ConfigItem(keyName = "miningHopIfNoRock", name = "Hop if no rock", description = "Wereld hop (future)", section = miningSection, position = 3)
    default boolean miningHopIfNoRock() { return false; }

    @ConfigSection(
            name = "Woodcutting",
            description = "Woodcutting instellingen",
            position = 21,
            closedByDefault = true
    )
    String woodcuttingSection = "woodcuttingSection";

    @ConfigItem(keyName = "wcMode", name = "Mode", description = "Powerdrop / Bank / Bird nest focus", section = woodcuttingSection, position = 0)
    default WoodcuttingMode wcMode() { return WoodcuttingMode.POWERDROP; }

    @ConfigItem(keyName = "wcBirdNestPickup", name = "Pickup Nests", description = "Pak bird nests op", section = woodcuttingSection, position = 1)
    default boolean wcBirdNestPickup() { return true; }

    @ConfigItem(keyName = "wcUseSpec", name = "Dragon Axe Spec", description = "Gebruik spec bij 100%", section = woodcuttingSection, position = 2)
    default boolean wcUseSpec() { return true; }

    @ConfigSection(
            name = "Fishing",
            description = "Fishing instellingen",
            position = 22,
            closedByDefault = true
    )
    String fishingSection = "fishingSection";

    @ConfigItem(keyName = "fishingMode", name = "Mode", description = "POWERFISH / BANK", section = fishingSection, position = 0)
    default FishingMode fishingMode() { return FishingMode.POWERFISH; }

    @ConfigItem(keyName = "fishingMethod", name = "Fishing Method", description = "Choose specific fishing method or Auto for level-based progression", section = fishingSection, position = 1)
    default FishingMethod fishingMethod() { return FishingMethod.AUTO; }

    @ConfigItem(keyName = "fishingUseSpecHarpoon", name = "Use Spec Harpoon", description = "Harpoon special gebruiken", section = fishingSection, position = 2)
    default boolean fishingUseSpecHarpoon() { return true; }


    @ConfigSection(
            name = "Hunter",
            description = "Hunter instellingen",
            position = 23,
            closedByDefault = true
    )
    String hunterSection = "hunterSection";

    @ConfigItem(keyName = "hunterMethod", name = "Method", description = "Auto / Chinchompa / Bird snare etc.", section = hunterSection, position = 0)
    default HunterMethod hunterMethod() { return HunterMethod.AUTO; }

    @ConfigItem(keyName = "hunterTrapCount", name = "Trap Count override", description = "0 = auto by level", section = hunterSection, position = 1)
    default int hunterTrapCount() { return 0; }

    @ConfigItem(keyName = "hunterAutoRelay", name = "Auto Relay", description = "Replant traps bij fail", section = hunterSection, position = 2)
    default boolean hunterAutoRelay() { return true; }

    @ConfigSection(
            name = "Farming",
            description = "Farming instellingen",
            position = 24,
            closedByDefault = true
    )
    String farmingSection = "farmingSection";

    @ConfigItem(keyName = "farmingRunMode", name = "Run Mode", description = "Herb / Tree / Fruit / All", section = farmingSection, position = 0)
    default FarmingRunMode farmingRunMode() { return FarmingRunMode.HERB_ONLY; }

    @ConfigItem(keyName = "farmingUseCompost", name = "Use Compost", description = "Gebruik (ultra) compost", section = farmingSection, position = 1)
    default CompostMode farmingUseCompost() { return CompostMode.SUPER; }

    @ConfigItem(keyName = "farmingBirdHouse", name = "Include Birdhouses", description = "Voeg birdhouse run toe", section = farmingSection, position = 2)
    default boolean farmingBirdHouse() { return false; }

    /* =====================================================
       ARTISAN / PROCESSING
       ===================================================== */

    @ConfigSection(
            name = "Smithing",
            description = "Smithing instellingen",
            position = 30,
            closedByDefault = true
    )
    String smithingSection = "smithingSection";

    @ConfigItem(keyName = "smithingMode", name = "Mode", description = "Anvil / Blast / Cannonballs", section = smithingSection, position = 0)
    default SmithingMode smithingMode() { return SmithingMode.ANVIL; }

    @ConfigItem(keyName = "smithingBarType", name = "Bar Type", description = "iron/steel/mith/addy/rune", section = smithingSection, position = 1)
    default String smithingBarType() { return "steel"; }

    @ConfigItem(keyName = "smithingUseCoalBag", name = "Coal Bag", description = "Gebruik coal bag (indien aanwezig)", section = smithingSection, position = 2)
    default boolean smithingUseCoalBag() { return true; }

    @ConfigSection(
            name = "Fletching",
            description = "Fletching instellingen",
            position = 31,
            closedByDefault = true
    )
    String fletchingSection = "fletchingSection";

    @ConfigItem(keyName = "fletchingMode", name = "Mode", description = "Logs->Longbow / Shafts / Darts", section = fletchingSection, position = 0)
    default FletchingMode fletchingMode() { return FletchingMode.LONGBOW; }

    @ConfigItem(keyName = "fletchingBankMode", name = "Bank Mode", description = "Bank / Sell GE (future)", section = fletchingSection, position = 1)
    default BankMode fletchingBankMode() { return BankMode.BANK; }

    @ConfigSection(
            name = "Crafting",
            description = "Crafting instellingen",
            position = 32,
            closedByDefault = true
    )
    String craftingSection = "craftingSection";

    @ConfigItem(keyName = "craftingMethod", name = "Method", description = "Glasses / Battlestaff / Gems", section = craftingSection, position = 0)
    default CraftingMethod craftingMethod() { return CraftingMethod.GEMS; }

    @ConfigItem(keyName = "craftingUsePortable", name = "Use Furnace (future)", description = "Placeholder voor boosts", section = craftingSection, position = 1)
    default boolean craftingUsePortable() { return false; }

    @ConfigSection(
            name = "Cooking",
            description = "Cooking instellingen",
            position = 33,
            closedByDefault = true
    )
    String cookingSection = "cookingSection";

    @ConfigItem(keyName = "cookingMode", name = "Mode", description = "Range / Fire / Hosidius", section = cookingSection, position = 0)
    default CookingMode cookingMode() { return CookingMode.RANGE; }

    @ConfigItem(keyName = "cookingGauntlets", name = "Cooking Gauntlets", description = "Zijn gauntlets aanwezig gebruiken", section = cookingSection, position = 1)
    default boolean cookingGauntlets() { return true; }

    @ConfigSection(
            name = "Firemaking",
            description = "Firemaking instellingen",
            position = 34,
            closedByDefault = true
    )
    String firemakingSection = "firemakingSection";

    @ConfigItem(keyName = "fmMode", name = "Mode", description = "Choose mode for firemaking training", section = firemakingSection, position = 0)
    default FiremakingLogType fmMode() { return FiremakingLogType.AUTO; }

    @ConfigSection(
            name = "Herblore",
            description = "Herblore instellingen",
            position = 35,
            closedByDefault = true
    )
    String herbloreSection = "herbloreSection";

    @ConfigItem(keyName = "herbloreMode", name = "Mode", description = "Clean / Unf / Mix / Make Tar", section = herbloreSection, position = 0)
    default HerbloreMode herbloreMode() { return HerbloreMode.CLEAN; }

    @ConfigItem(keyName = "herbloreUseSecondaries", name = "Use Secondaries", description = "Voegt secondaries toe bij mixen", section = herbloreSection, position = 1)
    default boolean herbloreUseSecondaries() { return true; }

    @ConfigSection(
            name = "Runecrafting",
            description = "Runecrafting instellingen",
            position = 36,
            closedByDefault = true
    )
    String runecraftingSection = "runecraftingSection";

    @ConfigItem(keyName = "rcMethod", name = "Method", description = "Altars / Abyss / GOTR / Lava", section = runecraftingSection, position = 0)
    default RunecraftMethod rcMethod() { return RunecraftMethod.ABYSS; }

    @ConfigItem(keyName = "rcUsePouches", name = "Use Pouches", description = "Gebruik pouches", section = runecraftingSection, position = 1)
    default boolean rcUsePouches() { return true; }

    @ConfigItem(keyName = "rcRepairWithNpc", name = "Use NPC Repair", description = "Use Dark Mage repair", section = runecraftingSection, position = 2)
    default boolean rcRepairWithNpc() { return true; }

    @ConfigSection(
            name = "Construction",
            description = "Construction instellingen",
            position = 37,
            closedByDefault = true
    )
    String constructionSection = "constructionSection";

    @ConfigItem(keyName = "constructionMethod", name = "Method", description = "Oak Larders / Mahogany / etc", section = constructionSection, position = 0)
    default ConstructionMethod constructionMethod() { return ConstructionMethod.OAK_LARDER; }

    @ConfigItem(keyName = "constructionUseServant", name = "Use Servant", description = "Demon butler etc.", section = constructionSection, position = 1)
    default boolean constructionUseServant() { return true; }

    @ConfigSection(
            name = "Slayer",
            description = "Slayer instellingen",
            position = 38,
            closedByDefault = true
    )
    String slayerSection = "slayerSection";

    @ConfigItem(keyName = "slayerTaskStrategy", name = "Task Strategy", description = "BLOCK / SKIP / EXTEND (placeholder)", section = slayerSection, position = 0)
    default SlayerTaskStrategy slayerTaskStrategy() { return SlayerTaskStrategy.BASIC; }

    @ConfigItem(keyName = "slayerUseCannon", name = "Use Cannon", description = "Cannon plaatsen indien mogelijk", section = slayerSection, position = 1)
    default boolean slayerUseCannon() { return false; }

    @ConfigSection(
            name = "Thieving",
            description = "Thieving instellingen",
            position = 39,
            closedByDefault = true
    )
    String thievingSection = "thievingSection";

    @ConfigItem(keyName = "thievingMethod", name = "Method", description = "Stalls / Knights / Ardy / Pyramid", section = thievingSection, position = 0)
    default ThievingMethod thievingMethod() { return ThievingMethod.STALLS; }

    @ConfigItem(keyName = "thievingFoodBelow", name = "Eet onder HP%", description = "HP threshold", section = thievingSection, position = 1)
    default int thievingFoodBelow() { return 40; }

    @ConfigItem(keyName = "thievingUseDodgy", name = "Use Dodgy Necklace", description = "Dodgy necklace equip check", section = thievingSection, position = 2)
    default boolean thievingUseDodgy() { return true; }

    @ConfigSection(
            name = "Agility",
            description = "Agility instellingen",
            position = 40,
            closedByDefault = true
    )
    String agilitySection = "agilitySection";

    @ConfigItem(keyName = "agilityCourseMode", name = "Course Mode", description = "AUTO / BEST / SPECIFIEK", section = agilitySection, position = 0)
    default AgilityCourseMode agilityCourseMode() { return AgilityCourseMode.AUTO; }

    @ConfigItem(keyName = "agilityUseStamina", name = "Use Stamina", description = "Gebruik stamina potions", section = agilitySection, position = 1)
    default boolean agilityUseStamina() { return true; }

    @ConfigItem(keyName = "agilityLootMarks", name = "Loot Marks", description = "Pak Marks of Grace op", section = agilitySection, position = 2)
    default boolean agilityLootMarks() { return true; }

    /* =====================================================
       ENUM TYPES
       ===================================================== */

    // Combat / potions / prayers
    enum CombatStyle { ACCURATE, AGGRESSIVE, CONTROLLED, DEFENSIVE }
    enum PotionMode { NONE, SUPER, SUPER_COMBAT, RANGING, MAGIC, ABSORPTION }
    enum PrayerSupportMode { NONE, PROTECT_MELEE, PROTECT_MISSILES, PROTECT_MAGIC, PIETY, RIGOUR, AUGURY }
    enum MagicTrainingMode { LOW_ALCH, HIGH_ALCH, SPLASH, CRUMBLE_UNDEAD, STUN_ALCH, SUPERHEAT, TELE_GRAB }
    enum PrayerTrainingMode { BONES_ALTAR, GILDED_BURNT, ECTOFUNTUS, ENSOULED_HEADS }

    // Gathering
    enum MiningMode { POWERDROP, BANK }
    enum WoodcuttingMode { POWERDROP, BANK, NEST_FOCUS }
    enum FishingMode { POWERFISH, BANK }
    enum FishingMethod { AUTO, NET, BAIT, LURE, CAGE, HARPOON }
    enum HunterMethod { AUTO, BIRDS, CHINCHOMPA, SALAMANDER, HERBIBOAR }
    enum FarmingRunMode { HERB_ONLY, TREE_ONLY, FRUIT_ONLY, HERB_TREE, FULL_RUN }
    enum CompostMode { NONE, COMPOST, SUPER, ULTRA }

    // Artisan / processing
    enum SmithingMode { ANVIL, BLAST_FURNACE, CANNONBALLS }
    enum FletchingMode { SHAFTS, SHORTBOW, LONGBOW, DARTS, BOLTS }
    enum BankMode { BANK, SELL_GE }
    enum CraftingMethod { GEMS, GLASSES, BATTLESTAFF, POTTERY }
    enum CookingMode { FIRE, RANGE, HOSIDIUS }
    enum FiremakingLogType { AUTO, CAMPFIRE, REGULAR, NORMAL, OAK, WILLOW, MAPLE, YEW, MAGIC, TEAK, MAHOGANY, REDWOOD }
    enum HerbloreMode { CLEAN, UNFINISHED, MIX, TAR }
    enum RunecraftMethod { ABYSS, ALTAR_DIRECT, LAVA, GOTR, ZMI }
    enum ConstructionMethod { OAK_LARDER, OAK_DOOR, MAHOGANY_TABLE, GUILD_BENCH }
    enum SlayerTaskStrategy { BASIC, BLOCK_META, EXTEND_PREFERRED }
    enum ThievingMethod { STALLS, CAKES, MASTER_FARMER, BLACKJACK, PYRAMID_PLUNDER }
    enum AgilityCourseMode { AUTO, BEST_XP, GNOME, DRAYNOR, VARROCK, CANIFIS, APE_ATOLL, SEERS, POLLNIVNEACH, ARDY, RELLEKKA, PRIFDDINAS }

}