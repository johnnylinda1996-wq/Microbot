package net.runelite.client.plugins.microbot.muletest;

import net.runelite.client.config.*;

@ConfigGroup(MuleTestConfig.configGroup)
@ConfigInformation(
        "<html>" +
                "<p>This script automatically requests a mule when you reach a certain amount of GP.</p>" +
                "<p>Configure the GP threshold and mule location below.</p>" +
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
            keyName = "gpThreshold",
            name = "GP Threshold",
            description = "Amount of GP to trigger mule request",
            section = generalSection,
            position = 0
    )
    @Range(min = 10000, max = 10000000)
    default int gpThreshold() {
        return 100000;
    }

    @ConfigItem(
            keyName = "checkInterval",
            name = "Check Interval (seconds)",
            description = "How often to check GP amount",
            section = generalSection,
            position = 1
    )
    @Range(min = 5, max = 300)
    default int checkInterval() {
        return 10;
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
            description = "Automatically walk to mule location when threshold is reached",
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
