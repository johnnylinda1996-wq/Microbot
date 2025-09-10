package net.runelite.client.plugins.microbot.muletest;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.*;

@ConfigGroup(MuleTestConfig.configGroup)
@ConfigInformation(
        "<html>" +
                "<p>This script automatically requests a mule on a time interval.</p>" +
                "<p>Trading is disabled; this script uses DROP TRADE only.</p>" +
                "<p>It pauses other running bots during the mule process and resumes them after completion.</p>" +
                "<p>Configure the time interval and mule location below.</p>" +
                "<p>Make sure the Mule Bridge is running on localhost:8080</p>" +
                "</html>")
public interface MuleTestConfig extends Config {
    String configGroup = "MuleTest";

    @ConfigSection(
            name = "General Settings",
            description = "General mule test settings",
            position = 0
    )
    String generalSection = "general";

    @ConfigSection(
            name = "Selling Settings",
            description = "Configure item selling at Grand Exchange",
            position = 1
    )
    String sellingSection = "selling";

    @ConfigSection(
            name = "Mule Settings",
            description = "Configure mule behavior",
            position = 2
    )
    String muleSection = "mule";

    @ConfigItem(
            keyName = "muleIntervalHours",
            name = "Mule Interval (Hours)",
            description = "How many hours between mule requests (e.g., 2.5 for 2.5 hours)",
            section = generalSection,
            position = 0
    )
    default double muleIntervalHours() {
        return 2.0;
    }

    @ConfigItem(
            keyName = "returnLocation",
            name = "Return Location",
            description = "Location to return to after mule process (X,Y,Z)",
            section = generalSection,
            position = 1
    )
    default String returnLocation() {
        return "3164,3486,0"; // Grand Exchange by default
    }

    @ConfigItem(
            keyName = "muleTimerLocation",
            name = "Mule Timer Location",
            description = "Location where bot goes when mule timer expires (X,Y,Z)",
            section = generalSection,
            position = 2
    )
    default String muleTimerLocation() {
        return "3164,3486,0"; // Grand Exchange by default
    }

    @ConfigItem(
            keyName = "dropLocationX",
            name = "Drop Location X",
            description = "X coordinate where to drop items for mule collection",
            section = generalSection,
            position = 3
    )
    default int dropLocationX() {
        return 3164; // Grand Exchange X by default
    }

    @ConfigItem(
            keyName = "dropLocationY",
            name = "Drop Location Y",
            description = "Y coordinate where to drop items for mule collection",
            section = generalSection,
            position = 4
    )
    default int dropLocationY() {
        return 3486; // Grand Exchange Y by default
    }

    @ConfigItem(
            keyName = "dropLocationZ",
            name = "Drop Location Z",
            description = "Z coordinate (plane) where to drop items for mule collection",
            section = generalSection,
            position = 5
    )
    default int dropLocationZ() {
        return 0; // Ground level by default
    }

    @ConfigItem(
            keyName = "sellItemsFirst",
            name = "Sell Items Before Mule",
            description = "Sell inventory items at GE before requesting mule",
            section = sellingSection,
            position = 0
    )
    default boolean sellItemsFirst() {
        return false;
    }

    @ConfigItem(
            keyName = "sellItemIds",
            name = "Item IDs to Sell",
            description = "Comma-separated item IDs to sell (e.g., 314,315,316 for anchovy,trout,salmon)",
            section = sellingSection,
            position = 1
    )
    default String sellItemIds() {
        return "";
    }

    @ConfigItem(
            keyName = "sellItemNames",
            name = "Item Names to Sell",
            description = "Comma-separated item names to sell (alternative to IDs)",
            section = sellingSection,
            position = 2
    )
    default String sellItemNames() {
        return "";
    }

    @ConfigItem(
            keyName = "sellAtMarketPrice",
            name = "Sell at Market Price",
            description = "Sell at current market price (if false, sells at -5%)",
            section = sellingSection,
            position = 3
    )
    default boolean sellAtMarketPrice() {
        return false;
    }

    @ConfigItem(
            keyName = "withdrawAllFromBank",
            name = "Withdraw All From Bank",
            description = "Withdraw all matching items from bank (multiple inventories)",
            section = sellingSection,
            position = 4
    )
    default boolean withdrawAllFromBank() {
        return true;
    }

    @ConfigItem(
            keyName = "muleLocation",
            name = "Mule Location",
            description = "Where to meet the mule",
            section = muleSection,
            position = 0
    )
    default MuleLocation muleLocation() {
        return MuleLocation.GRAND_EXCHANGE;
    }

    @ConfigItem(
            keyName = "muleAccount",
            name = "Mule Account Name",
            description = "Username of the mule account",
            section = muleSection,
            position = 1
    )
    default String muleAccount() {
        return "MuleBot1";
    }

    @ConfigItem(
            keyName = "bridgeUrl",
            name = "Bridge URL",
            description = "URL of the mule bridge server",
            section = muleSection,
            position = 2
    )
    default String bridgeUrl() {
        return "http://localhost:8080";
    }

    @ConfigItem(
            keyName = "autoWalkToMule",
            name = "Auto Walk to Mule",
            description = "Automatically walk to mule location when the time interval is reached",
            section = muleSection,
            position = 3
    )
    default boolean autoWalkToMule() {
        return true;
    }

    @ConfigItem(
            keyName = "stopAfterMule",
            name = "Stop After Mule",
            description = "Stop the script after successful mule trade",
            section = muleSection,
            position = 4
    )
    default boolean stopAfterMule() {
        return false;
    }

    @ConfigItem(
            keyName = "world",
            name = "Specific World",
            description = "Specific world number to use",
            section = muleSection,
            position = 5
    )
    default int world() {
        return 360;
    }

    enum MuleLocation {
        GRAND_EXCHANGE("Grand Exchange", new int[]{3164, 3486, 0}),
        VARROCK_WEST_BANK("Varrock West Bank", new int[]{3185, 3436, 0}),
        LUMBRIDGE("Lumbridge", new int[]{3222, 3218, 0}),
        FALADOR("Falador", new int[]{2965, 3378, 0}),
        EDGEVILLE("Edgeville", new int[]{3094, 3493, 0});

        private final String displayName;
        private final int[] coordinates;

        MuleLocation(String displayName, int[] coordinates) {
            this.displayName = displayName;
            this.coordinates = coordinates;
        }

        public String getDisplayName() {
            return displayName;
        }

        public WorldPoint getWorldPoint() {
            return new WorldPoint(coordinates[0], coordinates[1], coordinates[2]);
        }

        public String getCoordinateString() {
            return coordinates[0] + "," + coordinates[1] + "," + coordinates[2];
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Helper method to parse return location
    default WorldPoint getReturnWorldPoint() {
        try {
            String[] parts = returnLocation().split(",");
            return new WorldPoint(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (Exception e) {
            return new WorldPoint(3164, 3486, 0); // Default to GE
        }
    }

    // Helper method to parse mule timer location
    default WorldPoint getMuleTimerWorldPoint() {
        try {
            String[] parts = muleTimerLocation().split(",");
            return new WorldPoint(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (Exception e) {
            return new WorldPoint(3164, 3486, 0); // Default to GE
        }
    }

    // Helper method to get drop location as WorldPoint
    default WorldPoint getDropLocationWorldPoint() {
        return new WorldPoint(dropLocationX(), dropLocationY(), dropLocationZ());
    }

    // Helper method to get drop location as coordinate string
    default String getDropLocationString() {
        return dropLocationX() + "," + dropLocationY() + "," + dropLocationZ();
    }
}
