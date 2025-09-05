package net.runelite.client.plugins.microbot.jpnl.accountbuilder;

import net.runelite.client.config.*;

/**
 * Account Builder Configuration
 *
 * Clean and organized configuration with dedicated tabs for each skill.
 * Use the GUI to add tasks, configure training methods here.
 */
@ConfigGroup("allInOneAio")
public interface AllInOneConfig extends Config {

    /* ===================== GENERAL SETTINGS ====================== */

    @ConfigSection(
            name = "⚙️ General",
            description = "Basic bot settings",
            position = 0
    )
    String generalSection = "generalSection";

    @ConfigItem(
            keyName = "antibanEnabled",
            name = "🛡️ Antiban",
            description = "Enable anti-detection features",
            section = generalSection,
            position = 0
    )
    default boolean antibanEnabled() { return true; }

    @ConfigItem(
            keyName = "debug",
            name = "🐛 Debug",
            description = "Show detailed logs",
            section = generalSection,
            position = 1
    )
    default boolean debug() { return false; }

    @ConfigItem(
            keyName = "autoStart",
            name = "🏃 Auto Start",
            description = "Start when plugin loads",
            section = generalSection,
            position = 2
    )
    default boolean autoStart() { return false; }

    /* ===================== COMBAT SKILLS ====================== */

    // ATTACK
    @ConfigSection(
            name = "🗡️ Attack",
            description = "Attack training settings",
            position = 10,
            closedByDefault = true
    )
    String attackSection = "attackSection";

    @ConfigItem(
            keyName = "attackStyle",
            name = "Combat Style",
            description = "Combat style for Attack training",
            section = attackSection,
            position = 0
    )
    default CombatStyle attackStyle() { return CombatStyle.ACCURATE; }

    @ConfigItem(
            keyName = "attackFoodHP",
            name = "Food HP%",
            description = "Eat food below this HP%",
            section = attackSection,
            position = 1
    )
    @Range(min = 10, max = 90)
    default int attackFoodHP() { return 40; }

    @ConfigItem(
            keyName = "attackUseSpec",
            name = "Use Special Attack",
            description = "Use weapon special attacks",
            section = attackSection,
            position = 2
    )
    default boolean attackUseSpec() { return true; }

    // STRENGTH
    @ConfigSection(
            name = "💪 Strength",
            description = "Strength training settings",
            position = 11,
            closedByDefault = true
    )
    String strengthSection = "strengthSection";

    @ConfigItem(
            keyName = "strengthStyle",
            name = "Combat Style",
            description = "Combat style for Strength training",
            section = strengthSection,
            position = 0
    )
    default CombatStyle strengthStyle() { return CombatStyle.AGGRESSIVE; }

    @ConfigItem(
            keyName = "strengthFoodHP",
            name = "Food HP%",
            description = "Eat food below this HP%",
            section = strengthSection,
            position = 1
    )
    @Range(min = 10, max = 90)
    default int strengthFoodHP() { return 40; }

    @ConfigItem(
            keyName = "strengthUseSpec",
            name = "Use Special Attack",
            description = "Use weapon special attacks",
            section = strengthSection,
            position = 2
    )
    default boolean strengthUseSpec() { return true; }

    // DEFENCE
    @ConfigSection(
            name = "🛡️ Defence",
            description = "Defence training settings",
            position = 12,
            closedByDefault = true
    )
    String defenceSection = "defenceSection";

    @ConfigItem(
            keyName = "defenceStyle",
            name = "Combat Style",
            description = "Combat style for Defence training",
            section = defenceSection,
            position = 0
    )
    default CombatStyle defenceStyle() { return CombatStyle.DEFENSIVE; }

    @ConfigItem(
            keyName = "defenceFoodHP",
            name = "Food HP%",
            description = "Eat food below this HP%",
            section = defenceSection,
            position = 1
    )
    @Range(min = 10, max = 90)
    default int defenceFoodHP() { return 40; }

    @ConfigItem(
            keyName = "defenceUseSpec",
            name = "Use Special Attack",
            description = "Use weapon special attacks",
            section = defenceSection,
            position = 2
    )
    default boolean defenceUseSpec() { return false; }

    // HITPOINTS
    @ConfigSection(
            name = "❤️ Hitpoints",
            description = "Hitpoints training settings",
            position = 13,
            closedByDefault = true
    )
    String hitpointsSection = "hitpointsSection";

    @ConfigItem(
            keyName = "hitpointsMethod",
            name = "Training Method",
            description = "How to train Hitpoints",
            section = hitpointsSection,
            position = 0
    )
    default String hitpointsMethod() { return "Combat"; }

    // RANGED
    @ConfigSection(
            name = "🏹 Ranged",
            description = "Ranged training settings",
            position = 14,
            closedByDefault = true
    )
    String rangedSection = "rangedSection";

    @ConfigItem(
            keyName = "rangedAmmo",
            name = "Ammo Type",
            description = "Ammunition to use",
            section = rangedSection,
            position = 0
    )
    default String rangedAmmo() { return "Iron arrows"; }

    @ConfigItem(
            keyName = "rangedFoodHP",
            name = "Food HP%",
            description = "Eat food below this HP%",
            section = rangedSection,
            position = 1
    )
    @Range(min = 10, max = 90)
    default int rangedFoodHP() { return 50; }

    @ConfigItem(
            keyName = "rangedUseSpec",
            name = "Use Special Attack",
            description = "Use ranged special attacks",
            section = rangedSection,
            position = 2
    )
    default boolean rangedUseSpec() { return false; }

    // PRAYER
    @ConfigSection(
            name = "🙏 Prayer",
            description = "Prayer training settings",
            position = 15,
            closedByDefault = true
    )
    String prayerSection = "prayerSection";

    @ConfigItem(
            keyName = "prayerMethod",
            name = "Training Method",
            description = "How to train Prayer",
            section = prayerSection,
            position = 0
    )
    default PrayerMethod prayerMethod() { return PrayerMethod.BONES_ALTAR; }

    // MAGIC
    @ConfigSection(
            name = "🔮 Magic",
            description = "Magic training settings",
            position = 16,
            closedByDefault = true
    )
    String magicSection = "magicSection";

    @ConfigItem(
            keyName = "magicMethod",
            name = "Training Method",
            description = "How to train Magic",
            section = magicSection,
            position = 0
    )
    default MagicMethod magicMethod() { return MagicMethod.HIGH_ALCH; }

    @ConfigItem(
            keyName = "magicSplashProtection",
            name = "Splash Protection",
            description = "Restart if no XP gained",
            section = magicSection,
            position = 1
    )
    default boolean magicSplashProtection() { return true; }

    /* ===================== GATHERING SKILLS ====================== */

    // MINING
    @ConfigSection(
            name = "⛏️ Mining",
            description = "Mining training settings",
            position = 20,
            closedByDefault = true
    )
    String miningSection = "miningSection";

    @ConfigItem(
            keyName = "miningMode",
            name = "Training Mode",
            description = "Power mine or bank ores",
            section = miningSection,
            position = 0
    )
    default GatheringMode miningMode() { return GatheringMode.POWER_DROP; }

    @ConfigItem(
            keyName = "miningOres",
            name = "Ore Types",
            description = "Preferred ores (comma separated)",
            section = miningSection,
            position = 1
    )
    default String miningOres() { return "iron,coal"; }

    @ConfigItem(
            keyName = "mining3Tick",
            name = "3-Tick Mining",
            description = "Use 3-tick mining method",
            section = miningSection,
            position = 2
    )
    default boolean mining3Tick() { return false; }

    // SMITHING
    @ConfigSection(
            name = "⚒️ Smithing",
            description = "Smithing training settings",
            position = 21,
            closedByDefault = true
    )
    String smithingSection = "smithingSection";

    @ConfigItem(
            keyName = "smithingMethod",
            name = "Training Method",
            description = "How to train Smithing",
            section = smithingSection,
            position = 0
    )
    default SmithingMethod smithingMethod() { return SmithingMethod.ANVIL; }

    @ConfigItem(
            keyName = "smithingBars",
            name = "Bar Type",
            description = "Type of bars to work with",
            section = smithingSection,
            position = 1
    )
    default String smithingBars() { return "Steel"; }

    // FISHING
    @ConfigSection(
            name = "🎣 Fishing",
            description = "Fishing training settings",
            position = 22,
            closedByDefault = true
    )
    String fishingSection = "fishingSection";

    @ConfigItem(
            keyName = "fishingMode",
            name = "Training Mode",
            description = "Power fish or bank fish",
            section = fishingSection,
            position = 0
    )
    default GatheringMode fishingMode() { return GatheringMode.POWER_DROP; }

    @ConfigItem(
            keyName = "fishingMethod",
            name = "Fishing Method",
            description = "Fishing technique to use",
            section = fishingSection,
            position = 1
    )
    default FishingMethod fishingMethod() { return FishingMethod.AUTO; }

    @ConfigItem(
            keyName = "fishingSpecial",
            name = "Use Harpoon Special",
            description = "Use special attack on harpoons",
            section = fishingSection,
            position = 2
    )
    default boolean fishingSpecial() { return true; }

    // COOKING
    @ConfigSection(
            name = "👨‍🍳 Cooking",
            description = "Cooking training settings",
            position = 23,
            closedByDefault = true
    )
    String cookingSection = "cookingSection";

    @ConfigItem(
            keyName = "cookingLocation",
            name = "Cooking Location",
            description = "Where to cook food",
            section = cookingSection,
            position = 0
    )
    default CookingLocation cookingLocation() { return CookingLocation.AUTO; }

    @ConfigItem(
            keyName = "cookingGauntlets",
            name = "Use Cooking Gauntlets",
            description = "Equip gauntlets to reduce burns",
            section = cookingSection,
            position = 1
    )
    default boolean cookingGauntlets() { return true; }

    // FIREMAKING
    @ConfigSection(
            name = "🔥 Firemaking",
            description = "Firemaking training settings",
            position = 24,
            closedByDefault = true
    )
    String firemakingSection = "firemakingSection";

    @ConfigItem(
            keyName = "firemakingLogs",
            name = "Log Type",
            description = "Type of logs to burn",
            section = firemakingSection,
            position = 0
    )
    default LogType firemakingLogs() { return LogType.AUTO; }

    // WOODCUTTING
    @ConfigSection(
            name = "🪓 Woodcutting",
            description = "Woodcutting training settings",
            position = 25,
            closedByDefault = true
    )
    String woodcuttingSection = "woodcuttingSection";

    @ConfigItem(
            keyName = "woodcuttingMode",
            name = "Training Mode",
            description = "Power chop or bank logs",
            section = woodcuttingSection,
            position = 0
    )
    default GatheringMode woodcuttingMode() { return GatheringMode.POWER_DROP; }

    @ConfigItem(
            keyName = "woodcuttingTrees",
            name = "Tree Type",
            description = "Type of trees to cut",
            section = woodcuttingSection,
            position = 1
    )
    default TreeType woodcuttingTrees() { return TreeType.AUTO; }

    @ConfigItem(
            keyName = "woodcuttingNests",
            name = "Collect Bird Nests",
            description = "Pick up bird nests",
            section = woodcuttingSection,
            position = 2
    )
    default boolean woodcuttingNests() { return true; }

    @ConfigItem(
            keyName = "woodcuttingSpecial",
            name = "Use Dragon Axe Special",
            description = "Use special attack for speed boost",
            section = woodcuttingSection,
            position = 3
    )
    default boolean woodcuttingSpecial() { return true; }

    /* ===================== ARTISAN SKILLS ====================== */

    // CRAFTING
    @ConfigSection(
            name = "✂️ Crafting",
            description = "Crafting training settings",
            position = 30,
            closedByDefault = true
    )
    String craftingSection = "craftingSection";

    @ConfigItem(
            keyName = "craftingMethod",
            name = "Training Method",
            description = "What to craft for training",
            section = craftingSection,
            position = 0
    )
    default CraftingMethod craftingMethod() { return CraftingMethod.GEMS; }

    // FLETCHING
    @ConfigSection(
            name = "🏹 Fletching",
            description = "Fletching training settings",
            position = 31,
            closedByDefault = true
    )
    String fletchingSection = "fletchingSection";

    @ConfigItem(
            keyName = "fletchingMethod",
            name = "Training Method",
            description = "What to fletch for training",
            section = fletchingSection,
            position = 0
    )
    default FletchingMethod fletchingMethod() { return FletchingMethod.LONGBOWS; }

    @ConfigItem(
            keyName = "fletchingBanking",
            name = "Banking Mode",
            description = "Bank items or sell on GE",
            section = fletchingSection,
            position = 1
    )
    default BankingMode fletchingBanking() { return BankingMode.BANK; }

    // HERBLORE
    @ConfigSection(
            name = "🧪 Herblore",
            description = "Herblore training settings",
            position = 32,
            closedByDefault = true
    )
    String herbloreSection = "herbloreSection";

    @ConfigItem(
            keyName = "herbloreMethod",
            name = "Training Method",
            description = "How to train Herblore",
            section = herbloreSection,
            position = 0
    )
    default HerbloreMethod herbloreMethod() { return HerbloreMethod.CLEAN; }

    @ConfigItem(
            keyName = "herbloreSecondaries",
            name = "Use Secondary Ingredients",
            description = "Add secondaries when mixing",
            section = herbloreSection,
            position = 1
    )
    default boolean herbloreSecondaries() { return true; }

    // RUNECRAFTING
    @ConfigSection(
            name = "🌟 Runecrafting",
            description = "Runecrafting training settings",
            position = 33,
            closedByDefault = true
    )
    String runecraftingSection = "runecraftingSection";

    @ConfigItem(
            keyName = "runecraftingMethod",
            name = "Training Method",
            description = "Runecrafting training method",
            section = runecraftingSection,
            position = 0
    )
    default RunecraftingMethod runecraftingMethod() { return RunecraftingMethod.ABYSS; }

    @ConfigItem(
            keyName = "runecraftingPouches",
            name = "Use Essence Pouches",
            description = "Use pouches for more essence",
            section = runecraftingSection,
            position = 1
    )
    default boolean runecraftingPouches() { return true; }

    @ConfigItem(
            keyName = "runecraftingRepair",
            name = "Repair Pouches",
            description = "Use NPCs to repair pouches",
            section = runecraftingSection,
            position = 2
    )
    default boolean runecraftingRepair() { return true; }

    // CONSTRUCTION
    @ConfigSection(
            name = "🏠 Construction",
            description = "Construction training settings",
            position = 34,
            closedByDefault = true
    )
    String constructionSection = "constructionSection";

    @ConfigItem(
            keyName = "constructionMethod",
            name = "Training Method",
            description = "What to build for training",
            section = constructionSection,
            position = 0
    )
    default ConstructionMethod constructionMethod() { return ConstructionMethod.OAK_LARDERS; }

    @ConfigItem(
            keyName = "constructionServant",
            name = "Use Servant",
            description = "Use butler for materials",
            section = constructionSection,
            position = 1
    )
    default boolean constructionServant() { return true; }

    /* ===================== SUPPORT SKILLS ====================== */

    // AGILITY
    @ConfigSection(
            name = "🏃‍♂️ Agility",
            description = "Agility training settings",
            position = 40,
            closedByDefault = true
    )
    String agilitySection = "agilitySection";

    @ConfigItem(
            keyName = "agilityCourse",
            name = "Course Selection",
            description = "Agility course to use",
            section = agilitySection,
            position = 0
    )
    default AgilityCourse agilityCourse() { return AgilityCourse.AUTO; }

    @ConfigItem(
            keyName = "agilityStamina",
            name = "Use Stamina Potions",
            description = "Use stamina pots for run energy",
            section = agilitySection,
            position = 1
    )
    default boolean agilityStamina() { return true; }

    @ConfigItem(
            keyName = "agilityMarks",
            name = "Collect Marks of Grace",
            description = "Pick up marks during rooftops",
            section = agilitySection,
            position = 2
    )
    default boolean agilityMarks() { return true; }

    // THIEVING
    @ConfigSection(
            name = "🕵️ Thieving",
            description = "Thieving training settings",
            position = 41,
            closedByDefault = true
    )
    String thievingSection = "thievingSection";

    @ConfigItem(
            keyName = "thievingMethod",
            name = "Training Method",
            description = "What to thieve for training",
            section = thievingSection,
            position = 0
    )
    default ThievingMethod thievingMethod() { return ThievingMethod.STALLS; }

    @ConfigItem(
            keyName = "thievingFoodHP",
            name = "Food HP%",
            description = "Eat food below this HP%",
            section = thievingSection,
            position = 1
    )
    @Range(min = 10, max = 90)
    default int thievingFoodHP() { return 40; }

    @ConfigItem(
            keyName = "thievingDodgy",
            name = "Use Dodgy Necklace",
            description = "Equip dodgy necklaces",
            section = thievingSection,
            position = 2
    )
    default boolean thievingDodgy() { return true; }

    // SLAYER
    @ConfigSection(
            name = "💀 Slayer",
            description = "Slayer training settings",
            position = 42,
            closedByDefault = true
    )
    String slayerSection = "slayerSection";

    @ConfigItem(
            keyName = "slayerStrategy",
            name = "Task Strategy",
            description = "How to handle Slayer tasks",
            section = slayerSection,
            position = 0
    )
    default SlayerStrategy slayerStrategy() { return SlayerStrategy.BASIC; }

    @ConfigItem(
            keyName = "slayerCannon",
            name = "Use Dwarf Cannon",
            description = "Deploy cannon for tasks",
            section = slayerSection,
            position = 1
    )
    default boolean slayerCannon() { return false; }

    // HUNTER
    @ConfigSection(
            name = "🏹 Hunter",
            description = "Hunter training settings",
            position = 43,
            closedByDefault = true
    )
    String hunterSection = "hunterSection";

    @ConfigItem(
            keyName = "hunterMethod",
            name = "Training Method",
            description = "What to hunt for training",
            section = hunterSection,
            position = 0
    )
    default HunterMethod hunterMethod() { return HunterMethod.AUTO; }

    @ConfigItem(
            keyName = "hunterTraps",
            name = "Trap Count",
            description = "Number of traps (0 = auto)",
            section = hunterSection,
            position = 1
    )
    @Range(min = 0, max = 5)
    default int hunterTraps() { return 0; }

    @ConfigItem(
            keyName = "hunterRelay",
            name = "Auto Trap Replacement",
            description = "Automatically replace traps",
            section = hunterSection,
            position = 2
    )
    default boolean hunterRelay() { return true; }

    // FARMING
    @ConfigSection(
            name = "🌱 Farming",
            description = "Farming training settings",
            position = 44,
            closedByDefault = true
    )
    String farmingSection = "farmingSection";

    @ConfigItem(
            keyName = "farmingRunType",
            name = "Run Type",
            description = "Type of farming run",
            section = farmingSection,
            position = 0
    )
    default FarmingRunType farmingRunType() { return FarmingRunType.HERB_ONLY; }

    @ConfigItem(
            keyName = "farmingCompost",
            name = "Compost Type",
            description = "Type of compost to use",
            section = farmingSection,
            position = 1
    )
    default CompostType farmingCompost() { return CompostType.SUPER; }

    @ConfigItem(
            keyName = "farmingBirdhouses",
            name = "Include Birdhouses",
            description = "Add birdhouse runs",
            section = farmingSection,
            position = 2
    )
    default boolean farmingBirdhouses() { return false; }

    /* ===================== MINIGAME SETTINGS ====================== */

    // PEST CONTROL
    @ConfigSection(
            name = "🦗 Pest Control",
            description = "Pest Control minigame settings",
            position = 50,
            closedByDefault = true
    )
    String pestControlSection = "pestControlSection";

    @ConfigItem(
            keyName = "pestControlPriority1",
            name = "NPC Priority 1",
            description = "What NPC to attack as first option",
            section = pestControlSection,
            position = 0
    )
    default PestControlNpc pestControlPriority1() { return PestControlNpc.PORTAL; }

    @ConfigItem(
            keyName = "pestControlPriority2",
            name = "NPC Priority 2",
            description = "What NPC to attack as second option",
            section = pestControlSection,
            position = 1
    )
    default PestControlNpc pestControlPriority2() { return PestControlNpc.SPINNER; }

    @ConfigItem(
            keyName = "pestControlPriority3",
            name = "NPC Priority 3",
            description = "What NPC to attack as third option",
            section = pestControlSection,
            position = 2
    )
    default PestControlNpc pestControlPriority3() { return PestControlNpc.BRAWLER; }

    @ConfigItem(
            keyName = "pestControlAlchInBoat",
            name = "Alch while waiting",
            description = "Alch items while waiting in boat",
            section = pestControlSection,
            position = 3
    )
    default boolean pestControlAlchInBoat() { return false; }

    @ConfigItem(
            keyName = "pestControlAlchItem",
            name = "Item to alch",
            description = "Item name to alch",
            section = pestControlSection,
            position = 4
    )
    default String pestControlAlchItem() { return ""; }

    @ConfigItem(
            keyName = "pestControlQuickPrayer",
            name = "Enable QuickPrayer",
            description = "Enable quick prayer during games",
            section = pestControlSection,
            position = 5
    )
    default boolean pestControlQuickPrayer() { return false; }

    @ConfigItem(
            keyName = "pestControlSpecAttack",
            name = "Use Special Attack on %",
            description = "What percentage to use Special Attack",
            section = pestControlSection,
            position = 6
    )
    @Range(min = 25, max = 100)
    default int pestControlSpecAttack() { return 100; }

    @ConfigItem(
            keyName = "pestControlWorld",
            name = "World",
            description = "Pest Control world to use",
            section = pestControlSection,
            position = 7
    )
    default int pestControlWorld() { return 344; }

    @ConfigItem(
            keyName = "pestControlAutoTravel",
            name = "Auto Travel to Pest Control",
            description = "Automatically travel to Pest Control when task starts",
            section = pestControlSection,
            position = 8
    )
    default boolean pestControlAutoTravel() { return true; }

    @ConfigItem(
            keyName = "pestControlInventorySetup",
            name = "Inventory Setup",
            description = "Name of Inventory Setup to load before starting (optional)",
            section = pestControlSection,
            position = 9
    )
    default String pestControlInventorySetup() { return ""; }

    // NIGHTMARE ZONE
    @ConfigSection(
            name = "💀 Nightmare Zone",
            description = "Nightmare Zone minigame settings",
            position = 51,
            closedByDefault = true
    )
    String nmzSection = "nmzSection";

    @ConfigItem(
            keyName = "nmzShowOverlay",
            name = "Show NMZ Overlay",
            description = "Display NMZ points and absorption overlay",
            section = nmzSection,
            position = 0
    )
    default boolean nmzShowOverlay() { return true; }

    @ConfigItem(
            keyName = "nmzShowPoints",
            name = "Show Points",
            description = "Display current NMZ points in overlay",
            section = nmzSection,
            position = 1
    )
    default boolean nmzShowPoints() { return true; }

    @ConfigItem(
            keyName = "nmzShowAbsorption",
            name = "Show Absorption",
            description = "Display absorption points in overlay",
            section = nmzSection,
            position = 2
    )
    default boolean nmzShowAbsorption() { return true; }

    @ConfigItem(
            keyName = "nmzAbsorptionThreshold",
            name = "Absorption Threshold",
            description = "Drink absorption potion below this amount",
            section = nmzSection,
            position = 3
    )
    @Range(min = 1, max = 1000)
    default int nmzAbsorptionThreshold() { return 50; }

    @ConfigItem(
            keyName = "nmzAbsorptionColorAboveThreshold",
            name = "Color Above Threshold",
            description = "Absorption text color when above threshold",
            section = nmzSection,
            position = 4
    )
    default java.awt.Color nmzAbsorptionColorAboveThreshold() { return java.awt.Color.YELLOW; }

    @ConfigItem(
            keyName = "nmzAbsorptionColorBelowThreshold",
            name = "Color Below Threshold",
            description = "Absorption text color when below threshold",
            section = nmzSection,
            position = 5
    )
    default java.awt.Color nmzAbsorptionColorBelowThreshold() { return java.awt.Color.RED; }

    @ConfigItem(
            keyName = "nmzOverloadEarlyWarningSeconds",
            name = "Overload Early Warning",
            description = "Warning seconds before overload expires",
            section = nmzSection,
            position = 6
    )
    @Range(min = 0, max = 300)
    default int nmzOverloadEarlyWarningSeconds() { return 30; }

    @ConfigItem(
            keyName = "nmzUseSpecialAttack",
            name = "Use Special Attack",
            description = "Use special attack when available",
            section = nmzSection,
            position = 7
    )
    default boolean nmzUseSpecialAttack() { return true; }

    @ConfigItem(
            keyName = "nmzUseQuickPrayer",
            name = "Use Quick Prayer",
            description = "Enable quick prayer during NMZ",
            section = nmzSection,
            position = 8
    )
    default boolean nmzUseQuickPrayer() { return false; }

    @ConfigItem(
            keyName = "nmzUsePowerUps",
            name = "Use Power-ups",
            description = "Automatically use power-ups",
            section = nmzSection,
            position = 9
    )
    default boolean nmzUsePowerUps() { return true; }

    @ConfigItem(
            keyName = "nmzPowerSurgeNotification",
            name = "Power Surge Notification",
            description = "Show notification for Power Surge",
            section = nmzSection,
            position = 10
    )
    default boolean nmzPowerSurgeNotification() { return true; }

    @ConfigItem(
            keyName = "nmzRecurrentDamageNotification",
            name = "Recurrent Damage Notification",
            description = "Show notification for Recurrent Damage",
            section = nmzSection,
            position = 11
    )
    default boolean nmzRecurrentDamageNotification() { return true; }

    @ConfigItem(
            keyName = "nmzZapperNotification",
            name = "Zapper Notification",
            description = "Show notification for Zapper",
            section = nmzSection,
            position = 12
    )
    default boolean nmzZapperNotification() { return true; }

    @ConfigItem(
            keyName = "nmzUltimateForceNotification",
            name = "Ultimate Force Notification",
            description = "Show notification for Ultimate Force",
            section = nmzSection,
            position = 13
    )
    default boolean nmzUltimateForceNotification() { return true; }

    /* ===================== INTERNAL ====================== */

    @ConfigItem(
            keyName = "queuePersistence",
            name = "Queue Data",
            description = "Internal queue storage",
            hidden = true
    )
    default String queuePersistence() { return ""; }

    /* ===================== ENUM DEFINITIONS ====================== */

    enum CombatStyle {
        ACCURATE("Accurate"),
        AGGRESSIVE("Aggressive"),
        CONTROLLED("Controlled"),
        DEFENSIVE("Defensive");

        private final String name;
        CombatStyle(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum GatheringMode {
        POWER_DROP("Power Drop"),
        BANK("Bank");

        private final String name;
        GatheringMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum TreeType {
        AUTO("Auto"),
        NORMAL("Normal"),
        OAK("Oak"),
        WILLOW("Willow"),
        MAPLE("Maple"),
        YEW("Yew"),
        MAGIC("Magic");

        private final String name;
        TreeType(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum LogType {
        AUTO("Auto"),
        NORMAL("Normal"),
        OAK("Oak"),
        WILLOW("Willow"),
        MAPLE("Maple"),
        YEW("Yew"),
        MAGIC("Magic");

        private final String name;
        LogType(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum SmithingMethod {
        ANVIL("Anvil"),
        BLAST_FURNACE("Blast Furnace"),
        CANNONBALLS("Cannonballs");

        private final String name;
        SmithingMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum FletchingMethod {
        SHORTBOWS("Shortbows"),
        LONGBOWS("Longbows"),
        DARTS("Darts"),
        ARROWS("Arrows");

        private final String name;
        FletchingMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum BankingMode {
        BANK("Bank"),
        SELL_GE("Sell on GE");

        private final String name;
        BankingMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum CookingLocation {
        AUTO("Auto"),
        FIRE("Fire"),
        RANGE("Range"),
        HOSIDIUS("Hosidius");

        private final String name;
        CookingLocation(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum MagicMethod {
        HIGH_ALCH("High Alch"),
        SPLASHING("Splashing"),
        STUN_ALCH("Stun Alch"),
        SUPERHEAT("Superheat");

        private final String name;
        MagicMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum PrayerMethod {
        BONES_ALTAR("Bones on Altar"),
        GILDED_ALTAR("Gilded Altar"),
        ECTOFUNTUS("Ectofuntus"),
        ENSOULED_HEADS("Ensouled Heads");

        private final String name;
        PrayerMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum FishingMethod {
        AUTO("Auto"),
        NET("Small Net"),
        BAIT("Bait Fishing"),
        LURE("Lure/Fly Fishing"),
        CAGE("Lobster Cage"),
        HARPOON("Harpoon");

        private final String name;
        FishingMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum CraftingMethod {
        GEMS("Gem Cutting"),
        GLASSMAKING("Glassmaking"),
        BATTLESTAVES("Battlestaves"),
        POTTERY("Pottery");

        private final String name;
        CraftingMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum HerbloreMethod {
        CLEAN("Cleaning Herbs"),
        UNFINISHED("Unfinished Potions"),
        COMPLETE("Complete Potions"),
        BARBARIAN("Barbarian Herblore");

        private final String name;
        HerbloreMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum RunecraftingMethod {
        ABYSS("Abyss"),
        DIRECT("Direct Access"),
        LAVA("Lava Runes"),
        ZMI("ZMI Altar");

        private final String name;
        RunecraftingMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum ConstructionMethod {
        OAK_LARDERS("Oak Larders"),
        OAK_DOORS("Oak Doors"),
        MAHOGANY_TABLES("Mahogany Tables"),
        GUILD_BENCHES("Guild Benches");

        private final String name;
        ConstructionMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum AgilityCourse {
        AUTO("Auto"),
        GNOME("Gnome Course"),
        BARBARIAN("Barbarian"),
        CANIFIS("Canifis"),
        SEERS("Seers Village"),
        POLLNIVNEACH("Pollnivneach"),
        RELLEKKA("Rellekka"),
        ARDOUGNE("Ardougne");

        private final String name;
        AgilityCourse(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum ThievingMethod {
        STALLS("Market Stalls"),
        KNIGHTS("Knights"),
        PYRAMID("Pyramid Plunder"),
        BLACKJACKING("Blackjacking");

        private final String name;
        ThievingMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum SlayerStrategy {
        BASIC("Basic Tasks"),
        EFFICIENT("Efficient XP"),
        PROFIT("Profit Focus"),
        BLOCK_SKIP("Block/Skip Meta");

        private final String name;
        SlayerStrategy(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum HunterMethod {
        AUTO("Auto"),
        BIRDS("Bird Snaring"),
        CHINCHOMPAS("Chinchompas"),
        SALAMANDERS("Salamanders"),
        HERBIBOAR("Herbiboar");

        private final String name;
        HunterMethod(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum FarmingRunType {
        HERB_ONLY("Herb Patches Only"),
        TREE_ONLY("Tree Patches Only"),
        FRUIT_ONLY("Fruit Trees Only"),
        HERB_TREE("Herbs + Trees"),
        FULL_RUN("Complete Farm Run");

        private final String name;
        FarmingRunType(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum CompostType {
        NONE("No Compost"),
        COMPOST("Regular Compost"),
        SUPER("Supercompost"),
        ULTRA("Ultracompost");

        private final String name;
        CompostType(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    enum PestControlNpc {
        PORTAL("Portal"),
        BRAWLER("Brawler"),
        SPINNER("Spinner");

        private final String name;
        PestControlNpc(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }
}
