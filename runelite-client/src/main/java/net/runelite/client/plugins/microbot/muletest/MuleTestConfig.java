package net.runelite.client.plugins.microbot.muletest;

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
            name = "Mule Settings",
            description = "Configure mule behavior",
            position = 1
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

    // World Selection Configuration
    @ConfigItem(
            keyName = "isMember",
            name = "Is Member Account",
            description = "Use member worlds for login",
            section = muleSection,
            position = 5
    )
    default boolean isMember() {
        return false;
    }

    @ConfigItem(
            keyName = "world",
            name = "Specific World",
            description = "Specific world number to use (ignored if random world is enabled)",
            section = muleSection,
            position = 6
    )
    default int world() {
        return 360;
    }

    @ConfigItem(
            keyName = "useRandomWorld",
            name = "Use Random World",
            description = "Use random world selection instead of specific world",
            section = muleSection,
            position = 7
    )
    default boolean useRandomWorld() {
        return false;
    }

    @ConfigItem(
            keyName = "autoLogin",
            name = "Auto Login",
            description = "Automatically login when disconnected",
            section = muleSection,
            position = 8
    )
    default boolean autoLogin() {
        return true;
    }

    @ConfigItem(
            keyName = "sellItemsFirst",
            name = "Sell Items Before Mule",
            description = "Sell inventory items at GE before requesting mule (future feature)",
            section = muleSection,
            position = 9
    )
    default boolean sellItemsFirst() {
        return false;
    }

    @ConfigItem(
            keyName = "sellItemIds",
            name = "Item IDs to Sell",
            description = "Comma-separated item IDs to sell (e.g., 314,315,316 for anchovy,trout,salmon)",
            section = muleSection,
            position = 10
    )
    default String sellItemIds() {
        return "";
    }

    @ConfigItem(
            keyName = "sellItemNames",
            name = "Item Names to Sell",
            description = "Comma-separated item names to sell (alternative to IDs)",
            section = muleSection,
            position = 11
    )
    default String sellItemNames() {
        return "";
    }

    enum MuleLocation {
        GRAND_EXCHANGE("Grand Exchange"),
        VARROCK_WEST_BANK("Varrock West Bank"),
        LUMBRIDGE("Lumbridge"),
        FALADOR("Falador");

        private final String displayName;

        MuleLocation(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
