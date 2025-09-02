package net.runelite.client.plugins.microbot.jpnl.accountbuilder.travel;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * Predefined useful travel locations. F2P locations are shown first in the GUI.
 */
public enum TravelLocation {
    // F2P LOCATIONS (f2p = true)
    LUMBRIDGE("Lumbridge", new WorldPoint(3222, 3218, 0), true),
    LUMBRIDGE_BANK("Lumbridge Bank", new WorldPoint(3208, 3220, 2), true),
    LUMBRIDGE_COW_FIELD("Lumbridge Cow Field", new WorldPoint(3256, 3266, 0), true),
    VARROCK_CENTER("Varrock", new WorldPoint(3210, 3424, 0), true),
    VARROCK_WEST_BANK("Varrock West Bank", new WorldPoint(3185, 3436, 0), true),
    VARROCK_EAST_BANK("Varrock East Bank", new WorldPoint(3254, 3421, 0), true),
    VARROCK_MUSEUM("Varrock Museum", new WorldPoint(3254, 3448, 0), true),
    GRAND_EXCHANGE("Grand Exchange", new WorldPoint(3164, 3479, 0), true),
    FALADOR("Falador", new WorldPoint(2965, 3380, 0), true),
    FALADOR_WEST_BANK("Falador West Bank", new WorldPoint(2946, 3368, 0), true),
    FALADOR_EAST_BANK("Falador East Bank", new WorldPoint(3009, 3355, 0), true),
    EDGEVILLE("Edgeville", new WorldPoint(3093, 3493, 0), true),
    MONASTERY("Monastery", new WorldPoint(3052, 3488, 0), true),
    AL_KHARID("Al Kharid", new WorldPoint(3293, 3184, 0), true),
    DRAYNOR("Draynor Village", new WorldPoint(3093, 3244, 0), true),
    PORT_SARIM("Port Sarim", new WorldPoint(3014, 3224, 0), true),
    RIMMINGTON("Rimmington", new WorldPoint(2957, 3214, 0), true),
    BARBARIAN_VILLAGE("Barbarian Village", new WorldPoint(3082, 3420, 0), true),
    GOBLIN_VILLAGE("Goblin Village", new WorldPoint(2956, 3506, 0), true),
    STRONGHOLD_SECURITY("Stronghold of Security", new WorldPoint(3081, 3421, 0), true),
    MINING_GUILD("Mining Guild", new WorldPoint(3016, 3358, 0), true),

    // P2P LOCATIONS (f2p = false)
    CAMELOT("Camelot", new WorldPoint(2757, 3477, 0), false),
    SEERS_VILLAGE("Seers' Village", new WorldPoint(2723, 3492, 0), false),
    SEERS_BANK("Seers' Village Bank", new WorldPoint(2725, 3493, 0), false),
    CATHERBY("Catherby", new WorldPoint(2814, 3446, 0), false),
    ARDOUGNE_CENTER("Ardougne", new WorldPoint(2662, 3306, 0), false),
    ARDOUGNE_WEST_BANK("Ardougne West Bank", new WorldPoint(2655, 3283, 0), false),
    YANILLE("Yanille", new WorldPoint(2605, 3094, 0), false),
    BURTHORPE("Burthorpe", new WorldPoint(2896, 3546, 0), false),
    ROGUES_DEN("Rogues' Den", new WorldPoint(2904, 3537, 0), false),
    CASTLE_WARS("Castle Wars", new WorldPoint(2440, 3090, 0), false),
    PEST_CONTROL("Pest Control", new WorldPoint(2658, 2670, 0), false),
    WINTERTODT_CAMP("Wintertodt Camp", new WorldPoint(1623, 3937, 0), false),
    NMZ("Nightmare Zone", new WorldPoint(2605, 3114, 0), false),
    SLAYER_TOWER("Slayer Tower", new WorldPoint(3428, 3536, 0), false),
    FREMENNIK_SLAYER_DUNGEON("Fremennik Slayer Dungeon", new WorldPoint(2808, 10002, 0), false),
    MAGE_TRAINING_ARENA("Mage Training Arena", new WorldPoint(3362, 3316, 0), false),
    FISHING_GUILD("Fishing Guild", new WorldPoint(2598, 3420, 0), false),
    WOODCUTTING_GUILD("Woodcutting Guild", new WorldPoint(1658, 3505, 0), false),
    FARMING_GUILD("Farming Guild", new WorldPoint(1248, 3732, 0), false),
    SHILO_VILLAGE("Shilo Village", new WorldPoint(2854, 2959, 0), false),
    APE_ATOLL("Ape Atoll", new WorldPoint(2746, 2741, 0), false),
    LUNAR_ISLE("Lunar Isle", new WorldPoint(2085, 3914, 0), false),
    MOTHERLOAD_MINE("Motherlode Mine", new WorldPoint(3760, 5666, 0), false), // legacy typo retained
    MOTHERLODE_MINE("Motherlode Mine (alt)", new WorldPoint(3760, 5666, 0), false),
    TZHAAR_CITY("TzHaar City", new WorldPoint(2480, 5175, 0), false),
    ZANARIS("Zanaris", new WorldPoint(2452, 4474, 0), false),
    ARCEUUS_LIBRARY("Arceuus Library", new WorldPoint(1630, 3863, 0), false),
    HOSIDIUS("Hosidius", new WorldPoint(1745, 3570, 0), false),
    LOVAKENGJ("Lovakengj", new WorldPoint(1504, 3839, 0), false),
    PISCARILIUS("Port Piscarilius", new WorldPoint(1809, 3732, 0), false),
    SHAYZIEN("Shayzien", new WorldPoint(1552, 3627, 0), false),
    KOUREND_CASTLE("Kourend Castle", new WorldPoint(1611, 3688, 0), false),
    BARROWS("Barrows", new WorldPoint(3565, 3313, 0), false),
    ZUL_ANDRA("Zul-Andra", new WorldPoint(2199, 3056, 0), false);

    @Getter
    private final String displayName;
    @Getter
    private final WorldPoint point;
    @Getter
    private final boolean f2p;

    TravelLocation(String displayName, WorldPoint point, boolean f2p) {
        this.displayName = displayName;
        this.point = point;
        this.f2p = f2p;
    }

    public String resourceKey() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
