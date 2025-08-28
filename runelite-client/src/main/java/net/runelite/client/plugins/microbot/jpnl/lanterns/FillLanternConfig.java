package net.runelite.client.plugins.microbot.jpnl.lanterns;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("LanternRefill")
public interface FillLanternConfig extends Config {

    @ConfigSection(
            name = "General Settings",
            description = "General bot settings",
            position = 0
    )
    String generalSection = "general";

    @ConfigSection(
            name = "Teleportation",
            description = "Teleportation settings",
            position = 1
    )
    String teleportSection = "teleport";

    @ConfigItem(
            keyName = "minLanterns",
            name = "Minimum Lanterns",
            description = "Minimum amount of bullseye lanterns to withdraw",
            position = 0,
            section = generalSection
    )
    default int minLanterns() {
        return 25;
    }

    @ConfigItem(
            keyName = "maxLanterns",
            name = "Maximum Lanterns",
            description = "Maximum amount of bullseye lanterns to withdraw",
            position = 1,
            section = generalSection
    )
    default int maxLanterns() {
        return 27;
    }

    @ConfigItem(
            keyName = "useStamina",
            name = "Use Stamina Potions",
            description = "Use stamina potions when run energy is low",
            position = 2,
            section = generalSection
    )
    default boolean useStamina() {
        return true;
    }

    @ConfigItem(
            keyName = "enableBreakHandler",
            name = "Enable Break Handler",
            description = "Enable the microbot break handler plugin",
            position = 3,
            section = generalSection
    )
    default boolean enableBreakHandler() {
        return true;
    }

    @ConfigItem(
            keyName = "enableAntiban",
            name = "Enable Anti-ban",
            description = "Enable microbot anti-ban features",
            position = 4,
            section = generalSection
    )
    default boolean enableAntiban() {
        return true;
    }

    @ConfigItem(
            keyName = "teleportMethod",
            name = "Teleport Method",
            description = "Method to teleport to Rimmington",
            position = 0,
            section = teleportSection
    )
    default TeleportMethod teleportMethod() {
        return TeleportMethod.FALADOR_TELEPORT;
    }

    @ConfigItem(
            keyName = "useHouseTeleport",
            name = "Use House Teleport for Banking",
            description = "Use house teleport to get to POH for banking (requires house in Rimmington)",
            position = 1,
            section = teleportSection
    )
    default boolean useHouseTeleport() {
        return false;
    }

    enum TeleportMethod {
        FALADOR_TELEPORT("Falador Teleport"),
        RIMMINGTON_TELEPORT("Rimmington Teleport"),
        HOUSE_TELEPORT("House Teleport"),
        WALKING("Walking");

        private final String name;

        TeleportMethod(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}