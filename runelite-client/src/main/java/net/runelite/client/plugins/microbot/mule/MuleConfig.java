package net.runelite.client.plugins.microbot.mule;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("MuleConfig")
public interface MuleConfig extends Config {

    @ConfigItem(
            keyName = "bridgeUrl",
            name = "Bridge URL",
            description = "URL of the mule bridge server"
    )
    default String bridgeUrl() {
        return "http://localhost:8080";
    }

    @ConfigItem(
            keyName = "defaultLocationX",
            name = "Default X Coordinate",
            description = "Default X coordinate for trading location"
    )
    default int defaultLocationX() {
        return 3164; // Grand Exchange X
    }

    @ConfigItem(
            keyName = "defaultLocationY",
            name = "Default Y Coordinate",
            description = "Default Y coordinate for trading location"
    )
    default int defaultLocationY() {
        return 3486; // Grand Exchange Y
    }

    @ConfigItem(
            keyName = "defaultLocationZ",
            name = "Default Z Coordinate (Plane)",
            description = "Default Z coordinate (plane) for trading location"
    )
    default int defaultLocationZ() {
        return 0; // Ground level
    }

    @ConfigItem(
            keyName = "pollInterval",
            name = "Poll Interval (seconds)",
            description = "How often to check for new requests"
    )
    default int pollInterval() {
        return 5;
    }

    @ConfigItem(
            keyName = "logoutAfterTrade",
            name = "Logout After Trade",
            description = "Automatically logout after completing a trade"
    )
    default boolean logoutAfterTrade() {
        return false;
    }

    @ConfigItem(
            keyName = "maxTradeWaitTime",
            name = "Max Trade Wait Time (minutes)",
            description = "Maximum time to wait for a trade request"
    )
    default int maxTradeWaitTime() {
        return 5;
    }

    @ConfigItem(
            keyName = "enableDebugOverlay",
            name = "Enable Debug Overlay",
            description = "Show debug information on screen"
    )
    default boolean enableDebugOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "showBridgeGui",
            name = "Show Bridge GUI",
            description = "Show small GUI window for bridge status and controls"
    )
    default boolean showBridgeGui() {
        return true;
    }

    // Helper method to get default location as WorldPoint
    default String getDefaultLocationString() {
        return defaultLocationX() + "," + defaultLocationY() + "," + defaultLocationZ();
    }
}
